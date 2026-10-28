#!/usr/bin/env python3
"""
InChef — Intelligent Indian Recipe Dataset Importer
Imports recipe data from Excel (IndianFoodDatasetXLS.xlsx or sample) into MySQL.
Supports resume, dry-run, batching, unit normalization, and non-destructive upserts.
"""

import os
import re
import sys
import argparse
import pandas as pd
import pymysql

# Unit normalization mapping
UNIT_MAP = {
    'g': 'GRAMS', 'gm': 'GRAMS', 'gms': 'GRAMS', 'gram': 'GRAMS', 'grams': 'GRAMS',
    'kg': 'KILOGRAMS', 'kgs': 'KILOGRAMS', 'kilo': 'KILOGRAMS', 'kilogram': 'KILOGRAMS', 'kilograms': 'KILOGRAMS',
    'ml': 'MILLILITERS', 'mls': 'MILLILITERS', 'milliliter': 'MILLILITERS', 'milliliters': 'MILLILITERS',
    'l': 'LITERS', 'ltr': 'LITERS', 'liter': 'LITERS', 'liters': 'LITERS',
    'cup': 'CUPS', 'cups': 'CUPS',
    'tbsp': 'TABLESPOONS', 'tbsps': 'TABLESPOONS', 'tablespoon': 'TABLESPOONS', 'tablespoons': 'TABLESPOONS', 'tbs': 'TABLESPOONS',
    'tsp': 'TEASPOONS', 'tsps': 'TEASPOONS', 'teaspoon': 'TEASPOONS', 'teaspoons': 'TEASPOONS',
    'pinch': 'PINCH', 'pinches': 'PINCH',
    'piece': 'PIECES', 'pieces': 'PIECES', 'pc': 'PIECES', 'pcs': 'PIECES',
    'clove': 'PIECES', 'cloves': 'PIECES', 'slice': 'PIECES', 'slices': 'PIECES',
    'sprig': 'PIECES', 'sprigs': 'PIECES', 'leaf': 'PIECES', 'leaves': 'PIECES',
    'stalk': 'PIECES', 'stalks': 'PIECES', 'bunch': 'PIECES', 'bunches': 'PIECES'
}

FRACTION_MAP = {
    '½': 0.5, '⅓': 0.333, '⅔': 0.667, '¼': 0.25, '¾': 0.75,
    '⅕': 0.2, '⅖': 0.4, '⅗': 0.6, '⅘': 0.8, '⅙': 0.167, '⅚': 0.833,
    '⅛': 0.125, '⅜': 0.375, '⅝': 0.625, '⅞': 0.875
}

CATEGORY_MAP = {
    "Spices & Seasonings": ["salt", "turmeric", "cumin", "coriander", "garam masala", "chilli", "pepper", "clove", "cardamom", "cinnamon", "hing", "mustard seeds", "fennel"],
    "Vegetables": ["onion", "tomato", "potato", "ginger", "garlic", "carrot", "capsicum", "spinach", "pea", "peas", "cauliflower", "cabbage", "brinjal", "eggplant", "coriander leaves", "mint", "curry leaves"],
    "Dairy & Eggs": ["milk", "curd", "yogurt", "paneer", "butter", "ghee", "cream", "cheese", "egg", "eggs"],
    "Grains & Flours": ["rice", "flour", "atta", "maida", "besan", "rava", "semolina", "oats", "poha", "dal", "lentil"],
    "Proteins": ["chicken", "mutton", "fish", "prawn", "tofu", "soya"],
    "Oils & Fats": ["oil", "mustard oil", "coconut oil", "olive oil"],
    "Pantry Staples": ["sugar", "jaggery", "water", "vinegar", "lemon juice", "tamarind"]
}

def guess_category(name):
    lower = name.lower()
    for cat, keywords in CATEGORY_MAP.items():
        for kw in keywords:
            if kw in lower:
                return cat
    return "Produce"

def normalize_quantity(raw_val):
    if not raw_val:
        return 1.0
    text = str(raw_val).strip()
    for char, dec in FRACTION_MAP.items():
        text = text.replace(char, f" {dec}")
    try:
        parts = text.split()
        total = sum(float(p) for p in parts if re.match(r'^-?\d+(?:\.\d+)?$', p))
        return total if total > 0 else 1.0
    except Exception:
        return 1.0

def parse_ingredient_line(raw_line):
    line = raw_line.strip()
    if not line:
        return None
    
    # Common pattern: "1 cup Basmati Rice" or "2 teaspoons Salt"
    pattern = r'^([\d\s\./½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞-]+)?\s*([a-zA-Z]+)?\s+(.+)$'
    m = re.match(pattern, line)
    
    quantity = 1.0
    unit = 'PIECES'
    name = line

    if m:
        qty_str = m.group(1)
        unit_str = m.group(2)
        rest = m.group(3)

        if qty_str:
            quantity = normalize_quantity(qty_str)
        if unit_str and unit_str.lower() in UNIT_MAP:
            unit = UNIT_MAP[unit_str.lower()]
            name = rest
        elif unit_str:
            name = f"{unit_str} {rest}"

    # Clean name
    name = re.sub(r'\(.*?\)', '', name).strip()
    name = re.sub(r',.*$', '', name).strip()
    name = ' '.join(w.capitalize() for w in name.split())

    if not name or len(name) < 2:
        return None

    return {
        "name": name,
        "quantity": quantity,
        "unit": unit,
        "notes": line
    }

def main():
    parser = argparse.ArgumentParser(description="InChef Indian Food Dataset Importer")
    parser.add_argument("--dataset", "-d", help="Path to Excel dataset (.xlsx)")
    parser.add_argument("--host", default=os.environ.get("DB_HOST", "localhost"), help="MySQL host")
    parser.add_argument("--port", type=int, default=int(os.environ.get("DB_PORT", 3306)), help="MySQL port")
    parser.add_argument("--user", default=os.environ.get("DB_USERNAME", "root"), help="MySQL username")
    parser.add_argument("--password", default=os.environ.get("DB_PASSWORD", ""), help="MySQL password")
    parser.add_argument("--database", default=os.environ.get("DB_NAME", "inchef_db"), help="MySQL database")
    parser.add_argument("--limit", type=int, default=None, help="Max recipes to import")
    parser.add_argument("--dry-run", action="store_true", help="Validate and profile without database writes")
    args = parser.parse_args()

    # Determine dataset path
    dataset_path = args.dataset or os.environ.get("INCHEF_DATASET_PATH")
    if not dataset_path or not os.path.exists(dataset_path):
        sample_path = os.path.join("data", "samples", "sample_recipes.xlsx")
        if os.path.exists(sample_path):
            print(f"Dataset not specified or not found; falling back to sample dataset: {sample_path}")
            dataset_path = sample_path
        else:
            print(f"Error: Dataset not found at '{dataset_path}' and sample '{sample_path}' does not exist.")
            sys.exit(1)

    print(f"Reading dataset: {dataset_path}...")
    df = pd.read_excel(dataset_path, nrows=args.limit)
    print(f"Loaded {len(df)} rows from dataset.")

    if args.dry_run:
        print("[DRY-RUN] Validating rows and ingredients...")
        valid_rows = 0
        ingredients_found = set()
        for _, row in df.iterrows():
            title = str(row.get('RecipeName', '')).strip()
            if not title or title.lower() == 'nan':
                continue
            valid_rows += 1
            raw_ing = str(row.get('TranslatedIngredients', row.get('Ingredients', '')))
            for part in raw_ing.split(','):
                parsed = parse_ingredient_line(part)
                if parsed:
                    ingredients_found.add(parsed['name'])
        print(f"[DRY-RUN] Valid recipes: {valid_rows}/{len(df)}")
        print(f"[DRY-RUN] Unique ingredients identified: {len(ingredients_found)}")
        print("[DRY-RUN] Validation complete. No database modifications made.")
        return

    # Database connection
    print(f"Connecting to MySQL at {args.host}:{args.port}/{args.database}...")
    try:
        conn = pymysql.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password,
            database=args.database,
            charset='utf8mb4',
            autocommit=False
        )
    except Exception as e:
        print(f"Error connecting to MySQL: {e}")
        sys.exit(1)

    cursor = conn.cursor()
    print("Database connected successfully.")

    # Cache existing ingredients
    cursor.execute("SELECT id, name FROM ingredients")
    ing_cache = {row[1].lower(): row[0] for row in cursor.fetchall()}
    print(f"Existing ingredients in database: {len(ing_cache)}")

    # Cache existing recipes by source_recipe_id and title
    cursor.execute("SELECT id, title, source_recipe_id FROM recipes")
    existing_recipes = {row[1].lower(): row[0] for row in cursor.fetchall()}
    print(f"Existing recipes in database: {len(existing_recipes)}")

    imported_recipes = 0
    new_ingredients = 0

    for idx, row in df.iterrows():
        title = str(row.get('RecipeName', '')).strip()
        if not title or title.lower() == 'nan':
            continue

        if title.lower() in existing_recipes:
            continue

        trans_title = str(row.get('TranslatedRecipeName', title)).strip()
        cuisine = str(row.get('Cuisine', 'Indian')).strip()
        course = str(row.get('Course', 'Main Course')).strip()
        diet = str(row.get('Diet', 'Vegetarian')).strip()
        prep_time = int(row.get('PrepTimeInMins', 15)) if pd.notnull(row.get('PrepTimeInMins')) else 15
        cook_time = int(row.get('CookTimeInMins', 30)) if pd.notnull(row.get('CookTimeInMins')) else 30
        total_time = int(row.get('TotalTimeInMins', prep_time + cook_time)) if pd.notnull(row.get('TotalTimeInMins')) else prep_time + cook_time
        servings = int(row.get('Servings', 4)) if pd.notnull(row.get('Servings')) else 4
        instructions = str(row.get('TranslatedInstructions', row.get('Instructions', ''))).strip()
        source_url = str(row.get('URL', '')).strip()
        srno = str(row.get('Srno', idx + 1))

        dietary_type = 'VEGETARIAN'
        if 'non' in diet.lower():
            dietary_type = 'NON_VEGETARIAN'
        elif 'vegan' in diet.lower():
            dietary_type = 'VEGAN'

        meal_type = 'DINNER'
        if 'breakfast' in course.lower():
            meal_type = 'BREAKFAST'
        elif 'lunch' in course.lower():
            meal_type = 'LUNCH'
        elif 'snack' in course.lower() or 'appetizer' in course.lower():
            meal_type = 'SNACK'
        elif 'dessert' in course.lower():
            meal_type = 'DESSERT'

        difficulty = 'MEDIUM'
        if total_time <= 25:
            difficulty = 'EASY'
        elif total_time >= 60:
            difficulty = 'HARD'

        # Insert recipe
        cursor.execute("""
            INSERT INTO recipes (
                title, translated_recipe_name, description, instructions, cuisine,
                dietary_type, meal_type, difficulty, prep_time_minutes, cook_time_minutes,
                total_time_minutes, servings, spice_level, image_url, source_url,
                source_recipe_id, source_dataset, is_published, image_status, image_source_type,
                created_at, updated_at
            ) VALUES (
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, 1, 'FALLBACK', 'DEFAULT_FALLBACK',
                NOW(), NOW()
            )
        """, (
            title, trans_title, f"Traditional {cuisine} recipe for {title}.", instructions, cuisine,
            dietary_type, meal_type, difficulty, prep_time, cook_time,
            total_time, servings, 'MEDIUM', '/images/neutral_recipe_placeholder.svg', source_url,
            srno, 'IndianFoodDataset'
        ))
        recipe_id = cursor.lastrowid
        existing_recipes[title.lower()] = recipe_id
        imported_recipes += 1

        # Process ingredients
        raw_ing = str(row.get('TranslatedIngredients', row.get('Ingredients', '')))
        for part in raw_ing.split(','):
            parsed = parse_ingredient_line(part)
            if not parsed:
                continue

            ing_name = parsed['name']
            ing_key = ing_name.lower()

            if ing_key not in ing_cache:
                cat = guess_category(ing_name)
                cursor.execute("""
                    INSERT INTO ingredients (name, category, default_unit, created_at)
                    VALUES (%s, %s, %s, NOW())
                """, (ing_name, cat, parsed['unit']))
                ing_id = cursor.lastrowid
                ing_cache[ing_key] = ing_id
                new_ingredients += 1
            else:
                ing_id = ing_cache[ing_key]

            # Insert recipe_ingredient association
            cursor.execute("""
                INSERT INTO recipe_ingredients (
                    recipe_id, ingredient_id, quantity, unit, is_optional, notes
                ) VALUES (%s, %s, %s, %s, 0, %s)
            """, (recipe_id, ing_id, parsed['quantity'], parsed['unit'], parsed['notes']))

    conn.commit()
    cursor.close()
    conn.close()

    print(f"Import complete! Added {imported_recipes} new recipes and {new_ingredients} new ingredients.")

if __name__ == '__main__':
    main()

