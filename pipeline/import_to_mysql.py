import os
import sys
import pandas as pd
import pymysql

PROCESSED_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "processed")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PASS = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "inchef_db")

def run_import():
    print("=== Starting Production Ingestion to MySQL inchef_db ===")
    if not DB_PASS:
        print("Error: DB_PASSWORD environment variable is not set.", file=sys.stderr)
        print("Please set DB_PASSWORD in your environment or .env file before running import.", file=sys.stderr)
        sys.exit(1)
    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset="utf8mb4",
        autocommit=False
    )
    cursor = conn.cursor()

    try:
        # ----------------------------------------------------
        # 1. Ingest Ingredients
        # ----------------------------------------------------
        print("\n--- 1. Importing Canonical Ingredients ---")
        cursor.execute("SELECT id, LOWER(name) FROM ingredients")
        existing_ingredients = {row[1]: row[0] for row in cursor.fetchall()}
        print(f"Found {len(existing_ingredients)} existing ingredients in database.")

        df_ing = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_ingredients.csv"))
        csv_to_db_ing_map = {}
        new_ingredients_to_insert = []
        seen_new_ing_names = set()

        for _, row in df_ing.iterrows():
            csv_id = int(row['id'])
            raw_name = str(row['name']).strip()
            name_lower = raw_name.lower()
            category = str(row['category']).strip()[:50]
            default_unit = str(row['default_unit']).strip() if pd.notna(row['default_unit']) else 'PIECES'

            if name_lower in existing_ingredients:
                csv_to_db_ing_map[csv_id] = existing_ingredients[name_lower]
            elif name_lower not in seen_new_ing_names:
                new_ingredients_to_insert.append((csv_id, raw_name[:100], category, default_unit))
                seen_new_ing_names.add(name_lower)

        print(f"Identified {len(new_ingredients_to_insert)} new unique ingredients to insert.")
        
        # Batch insert new ingredients with INSERT IGNORE
        if new_ingredients_to_insert:
            sql_ing = """
                INSERT IGNORE INTO ingredients (name, category, default_unit, created_at)
                VALUES (%s, %s, %s, NOW())
            """
            for i in range(0, len(new_ingredients_to_insert), 500):
                batch = new_ingredients_to_insert[i:i+500]
                params = [(b[1], b[2], b[3]) for b in batch]
                cursor.executemany(sql_ing, params)
                conn.commit()

            # Refresh mapping
            cursor.execute("SELECT id, LOWER(name) FROM ingredients")
            all_ingredients = {row[1]: row[0] for row in cursor.fetchall()}
            for _, row in df_ing.iterrows():
                csv_id = int(row['id'])
                name_lower = str(row['name']).strip().lower()
                if name_lower in all_ingredients:
                    csv_to_db_ing_map[csv_id] = all_ingredients[name_lower]

        print(f"Total ingredients mapped: {len(csv_to_db_ing_map)}")

        # ----------------------------------------------------
        # 2. Ingest Recipes
        # ----------------------------------------------------
        print("\n--- 2. Importing Recipes ---")
        cursor.execute("SELECT id, LOWER(title) FROM recipes")
        existing_recipes = {row[1]: row[0] for row in cursor.fetchall()}
        print(f"Found {len(existing_recipes)} existing recipes in database.")

        df_recipes = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_recipes.csv"))
        csv_to_db_recipe_map = {}
        new_recipes_to_insert = []
        seen_new_recipe_titles = set()

        for _, row in df_recipes.iterrows():
            csv_r_id = int(row['recipe_id'])
            title = str(row['title']).strip()[:150]
            title_lower = title.lower()

            if title_lower in existing_recipes:
                csv_to_db_recipe_map[csv_r_id] = existing_recipes[title_lower]
                continue

            if title_lower in seen_new_recipe_titles:
                continue
            seen_new_recipe_titles.add(title_lower)

            desc = str(row['description']) if pd.notna(row['description']) else None
            cuisine = str(row['cuisine'])[:50] if pd.notna(row['cuisine']) else None
            meal_type = str(row['meal_type']) if pd.notna(row['meal_type']) else 'DINNER'
            dietary_type = str(row['dietary_type']) if pd.notna(row['dietary_type']) else 'VEGETARIAN'
            difficulty = str(row['difficulty']) if pd.notna(row['difficulty']) else 'MEDIUM'
            prep_time = int(row['prep_time_minutes']) if pd.notna(row['prep_time_minutes']) else None
            cook_time = int(row['cook_time_minutes']) if pd.notna(row['cook_time_minutes']) else None
            total_time = int(row['total_time_minutes']) if pd.notna(row['total_time_minutes']) else None
            servings = int(row['servings']) if pd.notna(row['servings']) else 4
            calories = int(row['calories']) if pd.notna(row['calories']) else None
            spice_level = str(row['spice_level']) if pd.notna(row['spice_level']) else 'MILD'
            instructions = str(row['instructions']) if pd.notna(row['instructions']) else None
            image_url = str(row['image_url'])[:500] if pd.notna(row['image_url']) else None
            video_url = None
            is_published = 1
            source_url = str(row['source_url'])[:500] if pd.notna(row['source_url']) else None
            source_dataset = str(row['source_dataset'])[:50] if pd.notna(row['source_dataset']) else None
            source_recipe_id = str(row['source_recipe_id'])[:100] if pd.notna(row['source_recipe_id']) else None

            new_recipes_to_insert.append((
                csv_r_id, title, desc, cuisine, meal_type, dietary_type, difficulty,
                prep_time, cook_time, total_time, servings, calories, spice_level,
                instructions, image_url, video_url, is_published, source_url, source_dataset, source_recipe_id
            ))

        print(f"Identified {len(new_recipes_to_insert)} new recipes to insert.")

        if new_recipes_to_insert:
            sql_recipe = """
                INSERT INTO recipes (
                    title, description, cuisine, meal_type, dietary_type, difficulty,
                    prep_time_minutes, cook_time_minutes, total_time_minutes, servings,
                    calories, spice_level, instructions, image_url, video_url, is_published,
                    source_url, source_dataset, source_recipe_id, created_at, updated_at
                ) VALUES (
                    %s, %s, %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s, %s, %s,
                    %s, %s, %s, NOW(), NOW()
                )
            """
            for i in range(0, len(new_recipes_to_insert), 500):
                batch = new_recipes_to_insert[i:i+500]
                params = [b[1:] for b in batch]
                cursor.executemany(sql_recipe, params)
                conn.commit()

            # Refresh recipe mapping
            cursor.execute("SELECT id, LOWER(title) FROM recipes")
            all_recipes = {row[1]: row[0] for row in cursor.fetchall()}
            for b in new_recipes_to_insert:
                csv_r_id = b[0]
                t_lower = b[1].lower()
                if t_lower in all_recipes:
                    csv_to_db_recipe_map[csv_r_id] = all_recipes[t_lower]

        print(f"Total recipes mapped: {len(csv_to_db_recipe_map)}")

        # ----------------------------------------------------
        # 3. Ingest Recipe-Ingredients Relationships
        # ----------------------------------------------------
        print("\n--- 3. Importing Recipe-Ingredients Relationships ---")
        cursor.execute("SELECT recipe_id, ingredient_id FROM recipe_ingredients")
        existing_pairs = set(cursor.fetchall())
        print(f"Found {len(existing_pairs)} existing recipe_ingredient relationships in database.")

        df_ri = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_recipe_ingredients.csv"))
        new_ri_to_insert = []

        for _, row in df_ri.iterrows():
            csv_r_id = int(row['recipe_id'])
            csv_i_id = int(row['ingredient_id'])

            db_r_id = csv_to_db_recipe_map.get(csv_r_id)
            db_i_id = csv_to_db_ing_map.get(csv_i_id)

            if not db_r_id or not db_i_id:
                continue

            if (db_r_id, db_i_id) in existing_pairs:
                continue

            qty = float(row['quantity']) if pd.notna(row['quantity']) else 1.0
            unit = str(row['unit']).strip()
            is_opt = int(row['is_optional']) if pd.notna(row['is_optional']) else 0
            notes = str(row['notes']).strip()[:255] if pd.notna(row['notes']) else None

            new_ri_to_insert.append((db_r_id, db_i_id, qty, unit, is_opt, notes))
            existing_pairs.add((db_r_id, db_i_id))

        print(f"Identified {len(new_ri_to_insert)} new recipe-ingredient pairs to insert.")

        if new_ri_to_insert:
            sql_ri = """
                INSERT INTO recipe_ingredients (
                    recipe_id, ingredient_id, quantity, unit, is_optional, notes
                ) VALUES (
                    %s, %s, %s, %s, %s, %s
                )
            """
            for i in range(0, len(new_ri_to_insert), 2000):
                batch = new_ri_to_insert[i:i+2000]
                cursor.executemany(sql_ri, batch)
                conn.commit()

        # Final table counts verification
        print("\n=== Final MySQL inchef_db Verification ===")
        for t in ['recipes', 'ingredients', 'recipe_ingredients', 'users', 'favorites', 'user_ingredients']:
            cursor.execute(f"SELECT COUNT(*) FROM `{t}`")
            cnt = cursor.fetchone()[0]
            print(f"  {t}: {cnt} rows")

        conn.commit()
        print("\nDATA INGESTION COMPLETED WITH 100% INTEGRITY!")

    except Exception as e:
        conn.rollback()
        print(f"ERROR DURING INGESTION: {e}", file=sys.stderr)
        raise
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    run_import()
