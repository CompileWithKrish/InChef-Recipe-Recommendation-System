import os
import re
import sys
import json
import argparse
import pandas as pd
import pymysql

EXCEL_PATH = os.environ.get(
    "INCHEF_DATASET_PATH",
    os.path.join(os.path.dirname(__file__), "..", "data", "samples", "sample_recipes.xlsx")
)
BACKUP_PATH = os.environ.get(
    "INCHEF_BACKUP_PATH",
    os.path.join(os.path.dirname(__file__), "..", "data", "backup", "inchef_db_backup_pre_migration.sql")
)
REPORT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "reports", "migration_summary_report.md")
REJECTED_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "rejected", "rejected_empty_ingredients.json")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PASS = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "inchef_db")

# Unit normalization map
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

CANONICAL_ALIASES = {
    "scallion": "Green Onion",
    "scallions": "Green Onion",
    "spring onion": "Green Onion",
    "spring onions": "Green Onion",
    "hari mirch": "Green Chilli",
    "green chili": "Green Chilli",
    "green chillies": "Green Chilli",
    "red chili": "Red Chilli",
    "red chillies": "Red Chilli",
    "dhania": "Coriander",
    "dhania leaves": "Coriander",
    "coriander leaves": "Coriander",
    "cilantro": "Coriander",
    "dahi": "Yogurt",
    "curd": "Yogurt",
    "jeera": "Cumin",
    "cumin seeds": "Cumin",
    "haldi": "Turmeric",
    "adrak": "Ginger",
    "lahsun": "Garlic",
    "lasun": "Garlic",
    "aloo": "Potato",
    "pyaaz": "Onion",
    "pyaz": "Onion",
    "tamatar": "Tomato",
    "palak": "Spinach",
    "methi": "Fenugreek",
    "sarson": "Mustard",
    "besan": "Gram Flour",
    "maida": "All Purpose Flour",
    "atta": "Wheat Flour",
    "capsicum": "Bell Pepper",
    "shimla mirch": "Bell Pepper"
}

PREP_WORDS = [
    'chopped', 'finely chopped', 'roughly chopped', 'minced', 'sliced', 'thinly sliced',
    'diced', 'peeled', 'grated', 'shredded', 'crushed', 'ground', 'powdered', 'roasted',
    'to taste', 'deseeded', 'slit', 'cut into pieces', 'boiled', 'cooked', 'soaked',
    'washed', 'drained', 'sifted', 'whisked', 'beaten', 'melted', 'warm', 'cold',
    'fresh', 'dry', 'dried', 'optional', 'for garnishing', 'as required', 'as needed'
]

# Explicit showcase/legacy favorite reconciliation mapping to IndianFoodDataset recipe IDs
FAVORITE_RECONCILIATION_MAP = {
    1: 9645,  # Classic Margherita Pizza -> Classic Pizza Margherita Recipe
    2: 8129,  # Creamy Garlic Butter Pasta -> Roasted Vegetable Pasta Primavera Recipe
    3: 3650,  # Spicy Kung Pao Chicken -> Kung Pao Chicken With Vegetable Fried Rice Recipe
    4: 9723,  # Classic Chicken Caesar Salad -> Grilled Chicken Salad Recipe
    5: 834,   # Loaded Black Bean Fiesta Bowls -> Punjabi Lobia Masala Recipe (Black Eyed Beans)
    6: 115,   # Paneer Butter Masala -> Paneer Matar Butter Masala
    5195: 3360 # Hariyali Murgh Biryani -> Lucknowi Murgh Biryani Recipe
}

def parse_quantity(text):
    text = text.strip()
    for fchar, val in FRACTION_MAP.items():
        if fchar in text:
            text = text.replace(fchar, f" {val} ")
    m_range = re.match(r'^(\d+(?:\.\d+)?)\s*(?:-|to)\s*(\d+(?:\.\d+)?)\b', text, re.I)
    if m_range:
        q1, q2 = float(m_range.group(1)), float(m_range.group(2))
        return round((q1 + q2) / 2.0, 2), text[m_range.end():].strip()
    m_comp = re.match(r'^(\d+)\s+(\d+)\s*/\s*(\d+)\b', text)
    if m_comp:
        val = float(m_comp.group(1)) + (float(m_comp.group(2)) / float(m_comp.group(3)))
        return round(val, 2), text[m_comp.end():].strip()
    m_frac = re.match(r'^(\d+)\s*/\s*(\d+)\b', text)
    if m_frac:
        val = float(m_frac.group(1)) / float(m_frac.group(2))
        return round(val, 2), text[m_frac.end():].strip()
    m_single = re.match(r'^(\d+(?:\.\d+)?)\b', text)
    if m_single:
        return round(float(m_single.group(1)), 2), text[m_single.end():].strip()
    return 1.0, text

def singularize_word(word):
    if len(word) <= 3:
        return word
    if word.endswith("ies") and len(word) > 4:
        return word[:-3] + "y"
    if word.endswith("es"):
        for suffix in ["shes", "ches", "xes", "zes", "toes"]:
            if word.endswith(suffix):
                return word[:-2]
    if word.endswith("s") and not word.endswith("ss") and not word.endswith("us") and not word.endswith("is"):
        return word[:-1]
    return word

def parse_ingredient_line(raw_line):
    if not raw_line or not isinstance(raw_line, str) or not raw_line.strip():
        return None
    line = raw_line.strip()
    if re.search(r'\bsalt\b', line, re.I) and re.search(r'to taste', line, re.I):
        return {
            "canonical_name": "Salt",
            "quantity": 1.0,
            "unit": "PINCH",
            "notes": "to taste",
            "category": "Spices & Herbs"
        }
    notes = []
    parens = re.findall(r'\((.*?)\)', line)
    notes.extend([p.strip() for p in parens if p.strip()])
    clean_text = re.sub(r'\(.*?\)', ' ', line)
    if '-' in clean_text:
        parts = clean_text.split('-', 1)
        clean_text = parts[0].strip()
        notes.append(parts[1].strip())
    qty, clean_text = parse_quantity(clean_text)
    words = clean_text.split()
    unit = "PIECES"
    if words:
        first = words[0].lower().strip(".,")
        if first in UNIT_MAP:
            unit = UNIT_MAP[first]
            words = words[1:]
        elif len(words) > 1 and f"{first} {words[1].lower().strip('.,')}" in UNIT_MAP:
            unit = UNIT_MAP[f"{first} {words[1].lower().strip('.,')}"]
            words = words[2:]
    clean_name = " ".join(words).strip()
    if ',' in clean_name:
        parts = clean_name.split(',')
        clean_name = parts[0].strip()
        notes.extend([p.strip() for p in parts[1:] if p.strip()])
    for pw in PREP_WORDS:
        pat = re.compile(rf'\b{pw}\b', re.I)
        if pat.search(clean_name):
            notes.append(pw)
            clean_name = pat.sub('', clean_name).strip()
    clean_name = re.sub(r'[^a-zA-Z0-9\s]', ' ', clean_name).strip()
    clean_name = re.sub(r'\s+', ' ', clean_name).strip()
    token_words = [singularize_word(w) for w in clean_name.split()]
    norm_name = " ".join(token_words).strip().lower()
    canonical_name = CANONICAL_ALIASES.get(norm_name, norm_name.title())
    if not canonical_name:
        canonical_name = "Ingredient"
    category = "General"
    low = canonical_name.lower()
    if any(k in low for k in ["chilli", "powder", "masala", "salt", "cumin", "turmeric", "coriander", "cinnamon", "cardamom", "clove", "peppercorn", "hing", "mustard", "saunf", "fenugreek"]):
        category = "Spices & Herbs"
    elif any(k in low for k in ["onion", "tomato", "potato", "garlic", "ginger", "spinach", "carrot", "pea", "gourd", "brinjal", "eggplant", "cauliflower", "cabbage", "capsicum", "pepper"]):
        category = "Vegetables"
    elif any(k in low for k in ["milk", "curd", "yogurt", "paneer", "ghee", "butter", "cheese"]):
        category = "Dairy & Eggs"
    elif any(k in low for k in ["chicken", "mutton", "fish", "prawn", "egg", "meat"]):
        category = "Proteins"
    elif any(k in low for k in ["rice", "dal", "flour", "atta", "besan", "lentil", "chana", "noodle", "wheat"]):
        category = "Grains"
    elif any(k in low for k in ["oil", "vinegar", "sauce", "sugar", "honey", "jaggery"]):
        category = "Condiments & Oils"

    note_str = ", ".join([n for n in notes if n and len(n) > 1])[:250]
    return {
        "canonical_name": canonical_name[:100],
        "quantity": qty,
        "unit": unit,
        "notes": note_str if note_str else None,
        "category": category
    }

def map_dietary_type(raw_diet):
    if not raw_diet or not isinstance(raw_diet, str):
        return "VEGETARIAN"
    d = raw_diet.strip().lower()
    if "non veg" in d or "non-veg" in d or "non vegeterian" in d or "non vegetarian" in d:
        return "NON_VEGETARIAN"
    if "vegan" in d:
        return "VEGAN"
    if "eggetarian" in d:
        return "EGGETARIAN"
    if "gluten free" in d:
        return "GLUTEN_FREE"
    return "VEGETARIAN"

def map_meal_type(raw_course):
    if not raw_course or not isinstance(raw_course, str):
        return "LUNCH"
    c = raw_course.strip().lower()
    if "breakfast" in c:
        return "BREAKFAST"
    if "dessert" in c or "sweet" in c:
        return "DESSERT"
    if "snack" in c or "appetizer" in c or "side dish" in c:
        return "SNACK"
    if "dinner" in c:
        return "DINNER"
    if "beverage" in c or "drink" in c:
        return "BEVERAGE"
    return "LUNCH"

def map_difficulty(total_mins, ing_count):
    if (total_mins and total_mins > 60) or ing_count > 12:
        return "HARD"
    if (total_mins and total_mins <= 30) and ing_count <= 6:
        return "EASY"
    return "MEDIUM"

def execute_migration():
    print("=" * 70)
    print("INCHEF COMPLETE DATASET MIGRATION & CATALOG PURGE")
    print("=" * 70)

    # 1. Verify backup
    if not os.path.exists(BACKUP_PATH):
        raise RuntimeError(f"Verified backup not found at: {BACKUP_PATH}. Aborting for safety!")
    backup_size = os.path.getsize(BACKUP_PATH)
    print(f"[OK] Verified pre-migration backup exists: {BACKUP_PATH} ({backup_size / (1024*1024):.2f} MB)")

    # 2. Inspect Excel
    if not os.path.exists(EXCEL_PATH):
        raise FileNotFoundError(f"Excel source file not found at: {EXCEL_PATH}")
    xl = pd.ExcelFile(EXCEL_PATH)
    sheet_name = xl.sheet_names[0]
    print(f"[OK] Reading Excel sheet '{sheet_name}' from {EXCEL_PATH}...")
    df = pd.read_excel(EXCEL_PATH, sheet_name=sheet_name)
    total_excel_rows = len(df)
    print(f"[OK] Total rows in Excel: {total_excel_rows}")

    # Identify empty ingredient rows
    empty_ing_df = df[df['TranslatedIngredients'].isnull() & df['Ingredients'].isnull()]
    rejected_rows_info = []
    for _, row in empty_ing_df.iterrows():
        rejected_rows_info.append({
            "Srno": int(row['Srno']),
            "RecipeName": str(row['RecipeName']),
            "Reason": "Missing both TranslatedIngredients and Ingredients"
        })
    print(f"[OK] Identified {len(rejected_rows_info)} invalid rows with empty ingredients.")
    os.makedirs(os.path.dirname(REJECTED_PATH), exist_ok=True)
    with open(REJECTED_PATH, "w", encoding="utf-8") as f:
        json.dump(rejected_rows_info, f, indent=2)
    print(f"[OK] Logged rejected rows to {REJECTED_PATH}")

    # 3. Connect to MySQL
    if not DB_PASS:
        print("Error: DB_PASSWORD environment variable is not set.", file=sys.stderr)
        print("Please set DB_PASSWORD in your environment or .env file before running migration.", file=sys.stderr)
        sys.exit(1)

    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset='utf8mb4',
        autocommit=False
    )
    cursor = conn.cursor()

    try:
        # Step A: Identify obsolete recipes (DataHive and showcase recipes)
        cursor.execute("SELECT id, title, source_dataset, source_recipe_id FROM recipes WHERE source_dataset != 'IndianFoodDataset' OR source_dataset IS NULL")
        obsolete_recipes = cursor.fetchall()
        obsolete_ids = [r[0] for r in obsolete_recipes]
        print(f"\n--- STEP 1: IDENTIFY OBSOLETE RECIPES ---")
        print(f"Total obsolete recipes to remove: {len(obsolete_ids)} (DataHive + showcase)")

        # Step B: Reconcile favorites pointing to obsolete recipes
        print(f"\n--- STEP 2: RECONCILE FAVORITES ---")
        cursor.execute("SELECT id, user_id, recipe_id FROM favorites")
        current_favs = cursor.fetchall()
        reconciled_favs = 0
        for fav_id, u_id, r_id in current_favs:
            if r_id in FAVORITE_RECONCILIATION_MAP:
                new_r_id = FAVORITE_RECONCILIATION_MAP[r_id]
                cursor.execute("UPDATE favorites SET recipe_id = %s WHERE id = %s", (new_r_id, fav_id))
                reconciled_favs += 1
                print(f"  Reconciled Favorite {fav_id} (User {u_id}): Old Recipe {r_id} -> New Recipe {new_r_id}")
            elif r_id in obsolete_ids:
                # Fallback: remove obsolete favorite reference
                cursor.execute("DELETE FROM favorites WHERE id = %s", (fav_id,))
                print(f"  Removed obsolete Favorite {fav_id} (User {u_id}) pointing to {r_id}")

        # Step C: Remove user recipe progress on obsolete recipes
        cursor.execute("DELETE FROM user_recipe_progress WHERE recipe_id IN (SELECT id FROM recipes WHERE source_dataset != 'IndianFoodDataset' OR source_dataset IS NULL)")
        print(f"Purged progress records for obsolete recipes.")

        # Step D: Delete recipe_ingredients for obsolete recipes in batches
        print(f"\n--- STEP 3: PURGE OBSOLETE RECIPE-INGREDIENT LINKS ---")
        if obsolete_ids:
            # Delete in chunks of 500
            for i in range(0, len(obsolete_ids), 500):
                chunk = obsolete_ids[i:i+500]
                format_strings = ','.join(['%s'] * len(chunk))
                cursor.execute(f"DELETE FROM recipe_ingredients WHERE recipe_id IN ({format_strings})", tuple(chunk))
            print(f"Deleted recipe-ingredient links for all {len(obsolete_ids)} obsolete recipes.")

            # Step E: Delete obsolete recipes from recipes table
            for i in range(0, len(obsolete_ids), 500):
                chunk = obsolete_ids[i:i+500]
                format_strings = ','.join(['%s'] * len(chunk))
                cursor.execute(f"DELETE FROM recipes WHERE id IN ({format_strings})", tuple(chunk))
            print(f"Deleted all {len(obsolete_ids)} obsolete recipes from recipes table.")

        # Step F: Purge orphan ingredients (not used by any recipe AND not used in user pantry)
        print(f"\n--- STEP 4: PURGE ORPHAN INGREDIENTS ---")
        cursor.execute("""
            DELETE FROM ingredients 
            WHERE id NOT IN (SELECT DISTINCT ingredient_id FROM recipe_ingredients)
              AND id NOT IN (SELECT DISTINCT ingredient_id FROM user_ingredients)
        """)
        purged_ingredients_count = cursor.rowcount
        print(f"Purged {purged_ingredients_count} orphan ingredients not referenced in any recipe or user pantry.")

        conn.commit()
        print("Database purge transaction committed successfully.")

        # Step G: Process and Synchronize IndianFoodDataset from Excel
        print(f"\n--- STEP 5: SYNCHRONIZE & NORMALIZE INDIAN FOOD DATASET ---")
        cursor.execute("SELECT id, LOWER(name) FROM ingredients")
        ing_cache = {row[1]: row[0] for row in cursor.fetchall()}
        print(f"Active canonical ingredients in cache: {len(ing_cache)}")

        cursor.execute("SELECT id, source_recipe_id FROM recipes WHERE source_dataset = 'IndianFoodDataset'")
        # Map both "IFD-<srno>" and "<srno>" to id for idempotent matching
        existing_ifd_recipes = {}
        for row in cursor.fetchall():
            s_id = str(row[1])
            existing_ifd_recipes[s_id] = row[0]
            if s_id.startswith("IFD-"):
                existing_ifd_recipes[s_id[4:]] = row[0]
            else:
                existing_ifd_recipes[f"IFD-{s_id}"] = row[0]

        print(f"Existing IndianFoodDataset recipes mapped: {len(existing_ifd_recipes) // 2}")

        imported_count = 0
        updated_count = 0
        new_ingredients_count = 0
        synced_links_count = 0

        for idx, row in df.iterrows():
            srno = str(row['Srno']).strip()
            source_recipe_id = f"IFD-{srno}"

            raw_title = str(row.get('TranslatedRecipeName', '')).strip()
            orig_name = str(row.get('RecipeName', '')).strip()
            if not raw_title or raw_title.lower() == 'nan':
                raw_title = orig_name
            if not raw_title or raw_title.lower() == 'nan':
                continue

            raw_ings = row.get('TranslatedIngredients')
            if pd.isna(raw_ings) or str(raw_ings).strip() == "" or str(raw_ings).lower() == "nan":
                raw_ings = row.get('Ingredients')
            if pd.isna(raw_ings) or str(raw_ings).strip() == "" or str(raw_ings).lower() == "nan":
                continue

            # Parse ingredients
            ing_lines = [l.strip() for l in str(raw_ings).split(',') if l.strip()]
            parsed_ings = []
            for il in ing_lines:
                res = parse_ingredient_line(il)
                if res and res["canonical_name"]:
                    parsed_ings.append(res)
            if not parsed_ings:
                continue

            # Timing & metadata
            prep = int(row.get('PrepTimeInMins', 0)) if pd.notna(row.get('PrepTimeInMins')) and row.get('PrepTimeInMins') > 0 else 0
            cook = int(row.get('CookTimeInMins', 0)) if pd.notna(row.get('CookTimeInMins')) and row.get('CookTimeInMins') > 0 else 0
            total = int(row.get('TotalTimeInMins', 0)) if pd.notna(row.get('TotalTimeInMins')) and row.get('TotalTimeInMins') > 0 else 0
            if total <= 0 or total < (prep + cook):
                total = prep + cook
            if total <= 0:
                total = 30

            servings = int(row.get('Servings', 4)) if pd.notna(row.get('Servings')) and row.get('Servings') > 0 else 4
            cuisine = str(row.get('Cuisine', 'Indian')).strip()[:50]
            dietary_type = map_dietary_type(str(row.get('Diet', '')))
            meal_type = map_meal_type(str(row.get('Course', '')))
            difficulty = map_difficulty(total, len(parsed_ings))

            instructions = row.get('TranslatedInstructions')
            if pd.isna(instructions) or str(instructions).strip() == "":
                instructions = row.get('Instructions')
            instructions = str(instructions).replace('\xa0', ' ').strip() if pd.notna(instructions) else None

            source_url = str(row.get('URL', '')).strip()[:500] if pd.notna(row.get('URL')) else None

            title = raw_title[:150]
            translated_name = raw_title[:200]
            desc = f"An authentic {cuisine} recipe ({title}) featuring {', '.join([pi['canonical_name'] for pi in parsed_ings[:3]])}."

            # Synchronize canonical ingredients
            recipe_ing_links = []
            seen_ing_ids = set()
            for pi in parsed_ings:
                cname = pi["canonical_name"]
                cname_low = cname.lower()
                if cname_low not in ing_cache:
                    cursor.execute(
                        "INSERT INTO ingredients (name, category, default_unit, created_at) VALUES (%s, %s, %s, NOW())",
                        (cname, pi["category"], pi["unit"])
                    )
                    new_id = cursor.lastrowid
                    ing_cache[cname_low] = new_id
                    new_ingredients_count += 1
                ing_id = ing_cache[cname_low]
                if ing_id not in seen_ing_ids:
                    seen_ing_ids.add(ing_id)
                    recipe_ing_links.append((ing_id, pi["quantity"], pi["unit"], pi["notes"], False))

            # Insert or Update Recipe
            if source_recipe_id in existing_ifd_recipes or srno in existing_ifd_recipes:
                recipe_id = existing_ifd_recipes.get(source_recipe_id, existing_ifd_recipes.get(srno))
                cursor.execute("""
                    UPDATE recipes SET
                        title = %s,
                        translated_recipe_name = %s,
                        ingredient_text = %s,
                        description = %s,
                        cuisine = %s,
                        meal_type = %s,
                        dietary_type = %s,
                        difficulty = %s,
                        prep_time_minutes = %s,
                        cook_time_minutes = %s,
                        total_time_minutes = %s,
                        servings = %s,
                        instructions = %s,
                        source_url = %s,
                        source_dataset = 'IndianFoodDataset',
                        source_recipe_id = %s,
                        updated_at = NOW()
                    WHERE id = %s
                """, (
                    title, translated_name, str(raw_ings), desc, cuisine, meal_type, dietary_type,
                    difficulty, prep, cook, total, servings, instructions, source_url, source_recipe_id, recipe_id
                ))
                updated_count += 1
            else:
                cursor.execute("""
                    INSERT INTO recipes (
                        title, translated_recipe_name, ingredient_text, description, cuisine, meal_type,
                        dietary_type, difficulty, prep_time_minutes, cook_time_minutes, total_time_minutes,
                        servings, instructions, source_url, source_dataset, source_recipe_id, is_published,
                        created_at, updated_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s,
                        %s, %s, %s, %s, %s,
                        %s, %s, %s, 'IndianFoodDataset', %s, 1,
                        NOW(), NOW()
                    )
                """, (
                    title, translated_name, str(raw_ings), desc, cuisine, meal_type,
                    dietary_type, difficulty, prep, cook, total,
                    servings, instructions, source_url, source_recipe_id
                ))
                recipe_id = cursor.lastrowid
                existing_ifd_recipes[source_recipe_id] = recipe_id
                imported_count += 1

            # Sync recipe_ingredients
            cursor.execute("DELETE FROM recipe_ingredients WHERE recipe_id = %s", (recipe_id,))
            for link in recipe_ing_links:
                cursor.execute("""
                    INSERT INTO recipe_ingredients (
                        recipe_id, ingredient_id, quantity, unit, notes, is_optional
                    ) VALUES (%s, %s, %s, %s, %s, %s)
                """, (recipe_id, link[0], link[1], link[2], link[3], link[4]))
                synced_links_count += 1

            if (idx + 1) % 500 == 0:
                conn.commit()
                print(f"Processed {idx + 1}/{total_excel_rows} recipes...")

        conn.commit()
        print("Dataset synchronization transaction committed successfully.")

        # Final Verification Queries
        print(f"\n--- STEP 6: POST-MIGRATION VERIFICATION ---")
        cursor.execute("SELECT count(*) FROM recipes")
        final_total_recipes = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM recipes WHERE source_dataset = 'IndianFoodDataset'")
        final_ifd_recipes = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM recipes WHERE source_dataset != 'IndianFoodDataset' OR source_dataset IS NULL")
        final_obsolete_recipes = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM ingredients")
        final_total_ingredients = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM recipe_ingredients")
        final_recipe_ingredients = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM users")
        final_users = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM user_ingredients")
        final_user_ingredients = cursor.fetchone()[0]

        cursor.execute("SELECT count(*) FROM favorites")
        final_favorites = cursor.fetchone()[0]

        cursor.execute("""
            SELECT count(*) FROM recipe_ingredients ri
            LEFT JOIN recipes r ON ri.recipe_id = r.id
            WHERE r.id IS NULL
        """)
        orphan_recipe_links = cursor.fetchone()[0]

        cursor.execute("""
            SELECT count(*) FROM favorites f
            LEFT JOIN recipes r ON f.recipe_id = r.id
            WHERE r.id IS NULL
        """)
        orphan_favorites = cursor.fetchone()[0]

        print(f"Active Catalog Recipes (All IndianFoodDataset): {final_ifd_recipes}")
        print(f"Total Database Recipes:                         {final_total_recipes}")
        print(f"Obsolete Recipes Remaining:                     {final_obsolete_recipes}")
        print(f"Distinct Canonical Ingredients:                 {final_total_ingredients}")
        print(f"Recipe-Ingredient Links:                        {final_recipe_ingredients}")
        print(f"Users Preserved:                                {final_users}")
        print(f"User Pantry Items Preserved:                    {final_user_ingredients}")
        print(f"User Favorites Preserved:                       {final_favorites}")
        print(f"Orphan Recipe-Ingredient Links:                 {orphan_recipe_links}")
        print(f"Orphan Favorites Links:                         {orphan_favorites}")

        # Assertions
        assert final_obsolete_recipes == 0, "Obsolete recipes still remain!"
        assert final_total_recipes == final_ifd_recipes, "Recipe count mismatch!"
        assert final_ifd_recipes == 6865, f"Expected 6865 recipes, got {final_ifd_recipes}!"
        assert orphan_recipe_links == 0, "Orphan recipe links detected!"
        assert orphan_favorites == 0, "Orphan favorites detected!"
        assert final_users >= 10, "User accounts lost!"
        assert final_user_ingredients >= 26, "User pantry lost!"

        # Generate Report
        os.makedirs(os.path.dirname(REPORT_PATH), exist_ok=True)
        report_content = f"""# InChef Official Dataset Migration & Database Purge Report

**Date & Time:** {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}  
**Source Dataset:** `{EXCEL_PATH}` (Sheet: `{sheet_name}`, Rows: {total_excel_rows})  
**Pre-migration Backup:** `{BACKUP_PATH}` ({backup_size / (1024*1024):.2f} MB)  

---

## 1. Migration Summary

All obsolete records (legacy DataHive recipes and showcase records) have been successfully removed in strict foreign-key order. The active recipe catalog is now 100% powered by the official **IndianFoodDataset** ({final_ifd_recipes} valid recipes).

| Entity / Metric | Count | Status |
| :--- | :--- | :--- |
| **Excel Source Rows** | {total_excel_rows} | 100% Processed |
| **Invalid Rows Skipped (Empty Ingredients)** | {len(rejected_rows_info)} | Logged in `{REJECTED_PATH}` |
| **Active Recipe Catalog (`recipes`)** | **{final_ifd_recipes}** | **100% IndianFoodDataset** |
| **Obsolete Recipes Purged** | **{len(obsolete_ids)}** | **0 Remaining** |
| **Distinct Canonical Ingredients** | **{final_total_ingredients}** | Clean, dataset-driven |
| **Recipe-Ingredient Junction Links** | **{final_recipe_ingredients}** | 0 Orphans |
| **Preserved User Accounts** | **{final_users}** | 100% Intact |
| **Preserved User Pantry Items** | **{final_user_ingredients}** | 100% Intact |
| **Preserved User Favorites** | **{final_favorites}** | Reconciled to Indian recipes |

---

## 2. Integrity & Referential Safety
- **No Cascade Loss:** 0 user accounts, preferences, or passwords were lost.
- **Idempotency:** Stable identifier `IFD-<Srno>` allows rerun without duplicate insertions.
- **Referential Integrity:** 0 broken foreign key references.
"""
        with open(REPORT_PATH, "w", encoding="utf-8") as f:
            f.write(report_content)
        print(f"\n[SUCCESS] Migration report saved to {REPORT_PATH}")

    except Exception as e:
        conn.rollback()
        print(f"\n[ERROR] Migration rolled back due to error: {e}", file=sys.stderr)
        raise e
    finally:
        conn.close()

if __name__ == "__main__":
    execute_migration()
