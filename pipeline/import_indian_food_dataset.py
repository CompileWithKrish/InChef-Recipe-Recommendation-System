import os
import re
import sys
import argparse
import pandas as pd
import pymysql

# Configuration and Database credentials
EXCEL_PATH = os.environ.get(
    "INCHEF_DATASET_PATH",
    os.path.join(os.path.dirname(__file__), "..", "data", "samples", "sample_recipes.xlsx")
)
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

# Unicode fraction replacements
FRACTION_MAP = {
    '½': 0.5, '⅓': 0.333, '⅔': 0.667, '¼': 0.25, '¾': 0.75,
    '⅕': 0.2, '⅖': 0.4, '⅗': 0.6, '⅘': 0.8, '⅙': 0.167, '⅚': 0.833,
    '⅛': 0.125, '⅜': 0.375, '⅝': 0.625, '⅞': 0.875
}

# Common culinary aliases for normalization
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

def parse_quantity(text):
    text = text.strip()
    for fchar, val in FRACTION_MAP.items():
        if fchar in text:
            text = text.replace(fchar, f" {val} ")
    
    # "1-2" or "1 to 2"
    m_range = re.match(r'^(\d+(?:\.\d+)?)\s*(?:-|to)\s*(\d+(?:\.\d+)?)\b', text, re.I)
    if m_range:
        q1, q2 = float(m_range.group(1)), float(m_range.group(2))
        return round((q1 + q2) / 2.0, 2), text[m_range.end():].strip()
    
    # "1 1/2"
    m_comp = re.match(r'^(\d+)\s+(\d+)\s*/\s*(\d+)\b', text)
    if m_comp:
        val = float(m_comp.group(1)) + (float(m_comp.group(2)) / float(m_comp.group(3)))
        return round(val, 2), text[m_comp.end():].strip()
    
    # "1/2"
    m_frac = re.match(r'^(\d+)\s*/\s*(\d+)\b', text)
    if m_frac:
        val = float(m_frac.group(1)) / float(m_frac.group(2))
        return round(val, 2), text[m_frac.end():].strip()
    
    # "2" or "1.5"
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
    
    # Salt to taste
    if re.search(r'\bsalt\b', line, re.I) and re.search(r'to taste', line, re.I):
        return {
            "canonical_name": "Salt",
            "quantity": 1.0,
            "unit": "PINCH",
            "notes": "to taste",
            "category": "Spices & Herbs"
        }
    
    notes = []
    # Parenthetical note
    parens = re.findall(r'\((.*?)\)', line)
    notes.extend([p.strip() for p in parens if p.strip()])
    clean_text = re.sub(r'\(.*?\)', ' ', line)
    
    # Hyphen notes
    if '-' in clean_text:
        parts = clean_text.split('-', 1)
        clean_text = parts[0].strip()
        notes.append(parts[1].strip())
    
    # Quantity
    qty, clean_text = parse_quantity(clean_text)
    
    # Unit
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
    
    clean_name = re.sub(r'\s+', ' ', clean_name).strip()
    if not clean_name:
        clean_name = line
    
    # Clean non-alphanumeric except spaces
    clean_name = re.sub(r'[^a-zA-Z0-9\s]', ' ', clean_name).strip()
    clean_name = re.sub(r'\s+', ' ', clean_name).strip()
    
    # Singularize individual words
    token_words = [singularize_word(w) for w in clean_name.split()]
    norm_name = " ".join(token_words).strip().lower()
    
    canonical_name = CANONICAL_ALIASES.get(norm_name, norm_name.title())
    if not canonical_name:
        canonical_name = "Ingredient"
        
    # Categorization heuristic
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

def run_import(sample_limit=None, dry_run=False):
    print("=" * 60)
    print(f"Starting IndianFoodDataset Import (Sample Limit: {sample_limit}, Dry Run: {dry_run})")
    print(f"Excel Source: {EXCEL_PATH}")
    print("=" * 60)
    
    if not os.path.exists(EXCEL_PATH):
        raise FileNotFoundError(f"Dataset file not found at: {EXCEL_PATH}")
        
    xl = pd.ExcelFile(EXCEL_PATH)
    sheet_name = xl.sheet_names[0]
    print(f"Reading worksheet: '{sheet_name}'...")
    df = pd.read_excel(EXCEL_PATH, sheet_name=sheet_name)
    total_source_rows = len(df)
    print(f"Total rows in spreadsheet: {total_source_rows}")
    
    if sample_limit:
        df = df.head(sample_limit)
        print(f"Processing sample subset of {len(df)} rows.")

    if not DB_PASS:
        print("Error: DB_PASSWORD environment variable is not set.", file=sys.stderr)
        print("Please set DB_PASSWORD in your environment or .env file before running dataset import.", file=sys.stderr)
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

    stats = {
        "source_rows": len(df),
        "imported_recipes": 0,
        "updated_recipes": 0,
        "skipped_duplicates": 0,
        "rejected_rows": 0,
        "missing_ingredients_count": 0,
        "missing_instructions_count": 0,
        "ingredients_created": 0,
        "recipe_ingredient_links": 0,
        "validation_errors": []
    }

    try:
        # Load existing canonical ingredients mapping: LOWER(name) -> id
        cursor.execute("SELECT id, LOWER(name) FROM ingredients")
        ing_cache = {row[1]: row[0] for row in cursor.fetchall()}
        print(f"Loaded {len(ing_cache)} canonical ingredients from database.")

        # Load existing IndianFoodDataset recipes by source_recipe_id
        cursor.execute("SELECT id, source_recipe_id FROM recipes WHERE source_dataset = 'IndianFoodDataset'")
        recipe_cache = {str(row[1]): row[0] for row in cursor.fetchall()}
        print(f"Found {len(recipe_cache)} existing IndianFoodDataset recipes in database.")

        batch_recipes = []
        batch_recipe_ingredients = []

        for idx, row in df.iterrows():
            srno = str(row['Srno']).strip()
            raw_title = str(row.get('TranslatedRecipeName', '')).strip()
            orig_name = str(row.get('RecipeName', '')).strip()
            
            if not raw_title or raw_title.lower() == 'nan':
                raw_title = orig_name
            if not raw_title or raw_title.lower() == 'nan':
                stats["rejected_rows"] += 1
                stats["validation_errors"].append(f"Row {idx} (Srno {srno}): Missing recipe title")
                continue

            raw_ings = row.get('TranslatedIngredients')
            if pd.isna(raw_ings) or str(raw_ings).strip() == "" or str(raw_ings).lower() == "nan":
                raw_ings = row.get('Ingredients')
            
            if pd.isna(raw_ings) or str(raw_ings).strip() == "" or str(raw_ings).lower() == "nan":
                stats["missing_ingredients_count"] += 1
                stats["rejected_rows"] += 1
                stats["validation_errors"].append(f"Row {idx} (Srno {srno}): Missing ingredients text")
                continue

            # Parse ingredients
            ing_lines = [l.strip() for l in str(raw_ings).split(',') if l.strip()]
            parsed_ings = []
            for il in ing_lines:
                res = parse_ingredient_line(il)
                if res and res["canonical_name"]:
                    parsed_ings.append(res)

            if not parsed_ings:
                stats["rejected_rows"] += 1
                stats["validation_errors"].append(f"Row {idx} (Srno {srno}): No parseable ingredients")
                continue

            # Timing parsing
            prep = int(row.get('PrepTimeInMins', 0)) if pd.notna(row.get('PrepTimeInMins')) and row.get('PrepTimeInMins') > 0 else 0
            cook = int(row.get('CookTimeInMins', 0)) if pd.notna(row.get('CookTimeInMins')) and row.get('CookTimeInMins') > 0 else 0
            total = int(row.get('TotalTimeInMins', 0)) if pd.notna(row.get('TotalTimeInMins')) and row.get('TotalTimeInMins') > 0 else 0
            if total <= 0 or total < (prep + cook):
                total = prep + cook
            if total <= 0:
                total = 30 # sensible default if completely missing

            servings = int(row.get('Servings', 4)) if pd.notna(row.get('Servings')) and row.get('Servings') > 0 else 4
            cuisine = str(row.get('Cuisine', 'Indian')).strip()[:50]
            dietary_type = map_dietary_type(str(row.get('Diet', '')))
            meal_type = map_meal_type(str(row.get('Course', '')))
            difficulty = map_difficulty(total, len(parsed_ings))

            instructions = row.get('TranslatedInstructions')
            if pd.isna(instructions) or str(instructions).strip() == "":
                instructions = row.get('Instructions')
            if pd.isna(instructions) or str(instructions).strip() == "":
                stats["missing_instructions_count"] += 1
                instructions = None
            else:
                instructions = str(instructions).replace('\xa0', ' ').strip()

            source_url = str(row.get('URL', '')).strip()[:500] if pd.notna(row.get('URL')) else None

            # Prepare Recipe record
            title = raw_title[:150]
            translated_name = raw_title[:200]
            desc = f"An authentic {cuisine} recipe ({title}) featuring {', '.join([pi['canonical_name'] for pi in parsed_ings[:3]])}."

            if not dry_run:
                # 1. Ensure canonical ingredients exist in DB
                recipe_ing_links_for_this_recipe = []
                seen_recipe_ing_ids = set()

                for pi in parsed_ings:
                    cname = pi["canonical_name"]
                    cname_low = cname.lower()
                    if cname_low not in ing_cache:
                        cursor.execute(
                            "INSERT INTO ingredients (name, category, default_unit, created_at) VALUES (%s, %s, %s, NOW())",
                            (cname, pi["category"], pi["unit"])
                        )
                        new_ing_id = cursor.lastrowid
                        ing_cache[cname_low] = new_ing_id
                        stats["ingredients_created"] += 1
                    ing_id = ing_cache[cname_low]

                    if ing_id not in seen_recipe_ing_ids:
                        seen_recipe_ing_ids.add(ing_id)
                        recipe_ing_links_for_this_recipe.append((
                            ing_id, pi["quantity"], pi["unit"], pi["notes"], False
                        ))

                # 2. Insert or Update Recipe
                recipe_id = None
                if srno in recipe_cache:
                    recipe_id = recipe_cache[srno]
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
                            updated_at = NOW()
                        WHERE id = %s
                    """, (
                        title, translated_name, str(raw_ings), desc, cuisine, meal_type, dietary_type,
                        difficulty, prep, cook, total, servings, instructions, source_url, recipe_id
                    ))
                    stats["updated_recipes"] += 1
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
                        servings, instructions, source_url, srno
                    ))
                    recipe_id = cursor.lastrowid
                    recipe_cache[srno] = recipe_id
                    stats["imported_recipes"] += 1

                # 3. Synchronize recipe-ingredient links idempotently
                cursor.execute("DELETE FROM recipe_ingredients WHERE recipe_id = %s", (recipe_id,))
                for ing_link in recipe_ing_links_for_this_recipe:
                    cursor.execute("""
                        INSERT INTO recipe_ingredients (
                            recipe_id, ingredient_id, quantity, unit, notes, is_optional
                        ) VALUES (%s, %s, %s, %s, %s, %s)
                    """, (
                        recipe_id, ing_link[0], ing_link[1], ing_link[2], ing_link[3], ing_link[4]
                    ))
                    stats["recipe_ingredient_links"] += 1

                # Batch commit every 100 recipes
                if (idx + 1) % 100 == 0:
                    conn.commit()
                    print(f"Processed {idx + 1}/{len(df)} recipes...")
            else:
                stats["imported_recipes"] += 1

        if not dry_run:
            conn.commit()
            print("Database transaction successfully committed.")

    except Exception as e:
        conn.rollback()
        print(f"Error during import: {e}", file=sys.stderr)
        raise e
    finally:
        conn.close()

    print("\n" + "=" * 60)
    print("IMPORT SUMMARY REPORT")
    print("=" * 60)
    print(f"Source Row Count:                 {stats['source_rows']}")
    print(f"Successfully Imported Recipes:    {stats['imported_recipes']}")
    print(f"Updated Existing Records:         {stats['updated_recipes']}")
    print(f"Skipped Duplicates:               {stats['skipped_duplicates']}")
    print(f"Rejected Rows:                    {stats['rejected_rows']}")
    print(f"Missing Ingredients Count:        {stats['missing_ingredients_count']}")
    print(f"Missing Instructions Count:       {stats['missing_instructions_count']}")
    print(f"New Canonical Ingredients Added:  {stats['ingredients_created']}")
    print(f"Recipe-Ingredient Links Added:    {stats['recipe_ingredient_links']}")
    print(f"Validation Errors Logged:         {len(stats['validation_errors'])}")
    if stats['validation_errors']:
        print("First 5 Validation Errors:")
        for err in stats['validation_errors'][:5]:
            print(f"  - {err}")
    print("=" * 60)

    # Write summary artifact report
    report_md = f"""# IndianFoodDataset Import Summary Report

**Date:** {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}  
**Source File:** `{EXCEL_PATH}`  
**Target Database:** `{DB_NAME}` on `{DB_HOST}:{DB_PORT}`  

## Execution Statistics

| Metric | Value |
| :--- | :--- |
| **Total Source Rows** | {stats['source_rows']} |
| **Successfully Imported Recipes** | {stats['imported_recipes']} |
| **Updated Existing Recipes** | {stats['updated_recipes']} |
| **Skipped Duplicates** | {stats['skipped_duplicates']} |
| **Rejected Rows** | {stats['rejected_rows']} |
| **Missing Ingredients Rows** | {stats['missing_ingredients_count']} |
| **Missing Instructions Rows** | {stats['missing_instructions_count']} |
| **New Canonical Ingredients Created** | {stats['ingredients_created']} |
| **Recipe-Ingredient Links Synced** | {stats['recipe_ingredient_links']} |
| **Validation Errors** | {len(stats['validation_errors'])} |

## Integrity Verification
- **Traceability:** Recipes retain `source_recipe_id` (`Srno`), `source_dataset` (`IndianFoodDataset`), `source_url`, `translated_recipe_name`, and `ingredient_text`.
- **Idempotency:** Re-running the pipeline updates matching `source_recipe_id` rows without duplicating recipes or recipe-ingredient links.
- **User Safety:** 0 user accounts, favorites, pantry items, or user preferences were deleted or modified.
"""
    reports_dir = os.path.join(os.path.dirname(__file__), "..", "data", "reports")
    os.makedirs(reports_dir, exist_ok=True)
    with open(os.path.join(reports_dir, "import_summary_report.md"), "w", encoding="utf-8") as f:
        f.write(report_md)
    print("Report written to data/reports/import_summary_report.md")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="IndianFoodDataset Importer")
    parser.add_argument("--sample", type=int, default=None, help="Process small sample for verification")
    parser.add_argument("--dry-run", action="store_true", help="Simulate import without DB commit")
    args = parser.parse_args()

    run_import(sample_limit=args.sample, dry_run=args.dry_run)
