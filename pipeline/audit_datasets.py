import os
import json
import csv
import ast
import pandas as pd
import numpy as np

RAW_DIR = os.environ.get("RAW_DATASET_DIR", os.path.join(os.path.dirname(__file__), "..", "data", "samples"))
AUDIT_DIR = os.environ.get("AUDIT_DIR", os.path.join(os.path.dirname(__file__), "..", "data", "audit"))
DATA_DIR = os.environ.get("DATA_DIR", os.path.join(os.path.dirname(__file__), "..", "data"))
os.makedirs(AUDIT_DIR, exist_ok=True)

stats = {}

print("=== Auditing Indian Food Dataset ===")
indian_path = os.path.join(RAW_DIR, "IndianFoodDatasetXLS.xlsx")
df_indian = pd.read_excel(indian_path, sheet_name=0)
indian_total = len(df_indian)

# Check missing & types
indian_cols = {}
for col in df_indian.columns:
    missing_cnt = int(df_indian[col].isna().sum())
    indian_cols[col] = {
        "dtype": str(df_indian[col].dtype),
        "missing_count": missing_cnt,
        "missing_pct": round(missing_cnt / indian_total * 100, 2)
    }

indian_dup_records = int(df_indian.duplicated().sum())
indian_dup_names = int(df_indian['TranslatedRecipeName'].str.strip().str.lower().duplicated().sum())

# Cuisines distribution
indian_cuisines = df_indian['Cuisine'].value_counts().head(20).to_dict()
# Courses distribution
indian_courses = df_indian['Course'].value_counts().head(20).to_dict()
# Diets distribution
indian_diets = df_indian['Diet'].value_counts().head(20).to_dict()

# Time availability
has_prep = int((df_indian['PrepTimeInMins'] > 0).sum())
has_cook = int((df_indian['CookTimeInMins'] > 0).sum())
has_total = int((df_indian['TotalTimeInMins'] > 0).sum())

# Ingredients & instructions
has_instructions = int((~df_indian['TranslatedInstructions'].isna() & (df_indian['TranslatedInstructions'].str.strip() != "")).sum())
has_ingredients = int((~df_indian['TranslatedIngredients'].isna() & (df_indian['TranslatedIngredients'].str.strip() != "")).sum())
has_url = int((~df_indian['URL'].isna() & (df_indian['URL'].str.strip() != "")).sum())
has_servings = int((~df_indian['Servings'].isna() & (df_indian['Servings'] > 0)).sum())

stats["indian_food_dataset"] = {
    "total_records": indian_total,
    "duplicate_records": indian_dup_records,
    "duplicate_recipe_names": indian_dup_names,
    "columns": indian_cols,
    "metrics": {
        "has_ingredients": has_ingredients,
        "has_instructions": has_instructions,
        "has_prep_time": has_prep,
        "has_cook_time": has_cook,
        "has_total_time": has_total,
        "has_servings": has_servings,
        "has_url": has_url,
        "has_image": 0,  # Dataset does not contain images directly
        "has_nutrition": 0
    },
    "top_cuisines": indian_cuisines,
    "top_courses": indian_courses,
    "top_diets": indian_diets
}

print(f"Indian Food Dataset: {indian_total} rows, {len(indian_cols)} columns")

print("\n=== Auditing CulinaryDB ===")
culinary_dir = os.path.join(RAW_DIR, "CulinaryDB")
culinary_stats = {}

# 01 Recipe Details
f01 = os.path.join(culinary_dir, "01_Recipe_Details.csv")
df_c01 = pd.read_csv(f01)
c01_total = len(df_c01)
c01_cuisines = df_c01['Cuisine'].value_counts().head(15).to_dict()
culinary_stats["recipe_details"] = {
    "total_records": c01_total,
    "unique_titles": int(df_c01['Title'].nunique()),
    "cuisines": c01_cuisines
}

# 02 Ingredients
f02 = os.path.join(culinary_dir, "02_Ingredients.csv")
df_c02 = pd.read_csv(f02)
c02_total = len(df_c02)
culinary_stats["ingredients"] = {
    "total_records": c02_total,
    "unique_entities": int(df_c02['Entity ID'].nunique()),
    "unique_names": int(df_c02['Aliased Ingredient Name'].nunique()),
    "categories": df_c02['Category'].value_counts().to_dict()
}

# 03 Compound Ingredients
f03 = os.path.join(culinary_dir, "03_Compound_Ingredients.csv")
df_c03 = pd.read_csv(f03)
c03_total = len(df_c03)
culinary_stats["compound_ingredients"] = {
    "total_records": c03_total,
    "compound_names": df_c03['Compound Ingredient Name'].tolist()[:25]
}

# 04 Recipe-Ingredients Aliases
f04 = os.path.join(culinary_dir, "04_Recipe-Ingredients_Aliases.csv")
df_c04 = pd.read_csv(f04)
c04_total = len(df_c04)
culinary_stats["aliases"] = {
    "total_records": c04_total,
    "unique_recipes": int(df_c04['Recipe ID'].nunique()),
    "unique_original_ingredients": int(df_c04['Original Ingredient Name'].nunique()),
    "unique_aliased_ingredients": int(df_c04['Aliased Ingredient Name'].nunique())
}

stats["culinary_db"] = culinary_stats
print(f"CulinaryDB: {c01_total} recipes, {c02_total} ingredients, {c03_total} compound ingredients, {c04_total} alias mappings")

print("\n=== Auditing DataHive (recipes-with-nutrition) ===")
datahive_path = os.path.join(RAW_DIR, "recipes-with-nutrition(DataHive).csv")

# Since DataHive is ~450MB, read in chunks to avoid blowing memory
chunksize = 25000
dh_total = 0
dh_dup_names = 0
dh_cols = None
dh_metrics = {
    "has_image": 0,
    "has_url": 0,
    "has_servings": 0,
    "has_calories": 0,
    "has_diet_labels": 0,
    "has_health_labels": 0,
    "has_cautions": 0,
    "has_cuisine_type": 0,
    "has_meal_type": 0,
    "has_dish_type": 0,
    "has_ingredients": 0,
    "has_ingredient_lines": 0,
    "has_total_nutrients": 0
}
cuisine_counter = {}
dish_counter = {}
meal_counter = {}
seen_names = set()
duplicate_name_count = 0

for chunk in pd.read_csv(datahive_path, chunksize=chunksize, low_memory=False):
    dh_total += len(chunk)
    if dh_cols is None:
        dh_cols = {c: {"dtype": str(chunk[c].dtype), "missing_count": 0} for c in chunk.columns}
    
    for c in chunk.columns:
        dh_cols[c]["missing_count"] += int(chunk[c].isna().sum())

    # Count metrics
    dh_metrics["has_image"] += int((~chunk["image_url"].isna() & (chunk["image_url"].str.strip() != "")).sum())
    dh_metrics["has_url"] += int((~chunk["url"].isna() & (chunk["url"].str.strip() != "")).sum())
    dh_metrics["has_servings"] += int((~chunk["servings"].isna() & (chunk["servings"] > 0)).sum())
    dh_metrics["has_calories"] += int((~chunk["calories"].isna() & (chunk["calories"] > 0)).sum())
    dh_metrics["has_diet_labels"] += int((~chunk["diet_labels"].isna() & (chunk["diet_labels"].str.strip() != "[]")).sum())
    dh_metrics["has_health_labels"] += int((~chunk["health_labels"].isna() & (chunk["health_labels"].str.strip() != "[]")).sum())
    dh_metrics["has_cautions"] += int((~chunk["cautions"].isna() & (chunk["cautions"].str.strip() != "[]")).sum())
    dh_metrics["has_cuisine_type"] += int((~chunk["cuisine_type"].isna() & (chunk["cuisine_type"].str.strip() != "[]")).sum())
    dh_metrics["has_meal_type"] += int((~chunk["meal_type"].isna() & (chunk["meal_type"].str.strip() != "[]")).sum())
    dh_metrics["has_dish_type"] += int((~chunk["dish_type"].isna() & (chunk["dish_type"].str.strip() != "[]")).sum())
    dh_metrics["has_ingredients"] += int((~chunk["ingredients"].isna() & (chunk["ingredients"].str.strip() != "[]")).sum())
    dh_metrics["has_ingredient_lines"] += int((~chunk["ingredient_lines"].isna() & (chunk["ingredient_lines"].str.strip() != "[]")).sum())
    dh_metrics["has_total_nutrients"] += int((~chunk["total_nutrients"].isna() & (chunk["total_nutrients"].str.strip() != "{}")).sum())

    # Cuisines distribution
    for c_val in chunk["cuisine_type"].dropna():
        try:
            items = ast.literal_eval(c_val) if isinstance(c_val, str) and c_val.startswith("[") else [str(c_val)]
            for it in items:
                it = str(it).strip().lower()
                cuisine_counter[it] = cuisine_counter.get(it, 0) + 1
        except Exception:
            pass

    # Check duplicate names
    for name in chunk["recipe_name"].dropna():
        n_clean = str(name).strip().lower()
        if n_clean in seen_names:
            duplicate_name_count += 1
        else:
            seen_names.add(n_clean)

for c in dh_cols:
    dh_cols[c]["missing_pct"] = round(dh_cols[c]["missing_count"] / dh_total * 100, 2)

stats["datahive_recipes"] = {
    "total_records": dh_total,
    "unique_recipe_names": len(seen_names),
    "duplicate_recipe_names": duplicate_name_count,
    "columns": dh_cols,
    "metrics": dh_metrics,
    "cuisines_distribution": dict(sorted(cuisine_counter.items(), key=lambda x: x[1], reverse=True)[:30])
}
print(f"DataHive: {dh_total} recipes, {len(seen_names)} unique titles")

# Save dataset_statistics.json
stats_path = os.path.join(AUDIT_DIR, "dataset_statistics.json")
with open(stats_path, "w", encoding="utf-8") as f:
    json.dump(stats, f, indent=2)
print(f"Saved stats to {stats_path}")

# Write field_mapping_preliminary.csv
field_mapping = [
    {"InChef_Target_Field": "id", "IndianFoodDataset": "Srno (or auto-gen)", "DataHive": "Row Index / URL hash", "CulinaryDB": "Recipe ID", "Transformation_Rule": "Auto-increment primary key in InChef"},
    {"InChef_Target_Field": "title", "IndianFoodDataset": "TranslatedRecipeName", "DataHive": "recipe_name", "CulinaryDB": "Title", "Transformation_Rule": "Trim, Title Case, clean HTML and quotes"},
    {"InChef_Target_Field": "description", "IndianFoodDataset": "Generated summary from title + course", "DataHive": "Dish/Meal description or generated", "CulinaryDB": "None", "Transformation_Rule": "Rich synthesized or native description"},
    {"InChef_Target_Field": "cuisine", "IndianFoodDataset": "Cuisine (normalized Indian/regional)", "DataHive": "cuisine_type (filtered Asian/Indian)", "CulinaryDB": "Cuisine", "Transformation_Rule": "Standardized Cuisine mapping (Indian, Chinese, Japanese, Thai, etc.)"},
    {"InChef_Target_Field": "meal_type", "IndianFoodDataset": "Course (mapped to MealType)", "DataHive": "meal_type (mapped to MealType)", "CulinaryDB": "None", "Transformation_Rule": "Map to BREAKFAST, LUNCH, DINNER, SNACK, DESSERT, BEVERAGE"},
    {"InChef_Target_Field": "dietary_type", "IndianFoodDataset": "Diet (mapped to DietaryType)", "DataHive": "diet_labels / health_labels", "CulinaryDB": "None", "Transformation_Rule": "Map to VEGETARIAN, VEGAN, NON_VEGETARIAN, EGGETARIAN, GLUTEN_FREE, KETO, DAIRY_FREE"},
    {"InChef_Target_Field": "difficulty", "IndianFoodDataset": "Derived from TotalTimeInMins & ingredients count", "DataHive": "Derived from total ingredients & prep", "CulinaryDB": "None", "Transformation_Rule": "EASY (<30m, <8 ing), HARD (>60m, >15 ing), else MEDIUM"},
    {"InChef_Target_Field": "prep_time_minutes", "IndianFoodDataset": "PrepTimeInMins", "DataHive": "NULL (unless structured)", "CulinaryDB": "None", "Transformation_Rule": "Direct integer minutes or NULL"},
    {"InChef_Target_Field": "cook_time_minutes", "IndianFoodDataset": "CookTimeInMins", "DataHive": "NULL (unless structured)", "CulinaryDB": "None", "Transformation_Rule": "Direct integer minutes or NULL"},
    {"InChef_Target_Field": "total_time_minutes", "IndianFoodDataset": "TotalTimeInMins", "DataHive": "NULL", "CulinaryDB": "None", "Transformation_Rule": "Prep + Cook if total is missing, else source total"},
    {"InChef_Target_Field": "servings", "IndianFoodDataset": "Servings", "DataHive": "servings", "CulinaryDB": "None", "Transformation_Rule": "Direct integer or default 4 if unparseable"},
    {"InChef_Target_Field": "calories", "IndianFoodDataset": "NULL", "DataHive": "calories / servings (per serving) or total", "CulinaryDB": "None", "Transformation_Rule": "Mapped from DataHive calories or calories per serving"},
    {"InChef_Target_Field": "spice_level", "IndianFoodDataset": "Derived from spice ingredients (chilli, etc.)", "DataHive": "Derived from ingredients or MILD", "CulinaryDB": "None", "Transformation_Rule": "MILD, MEDIUM, SPICY, EXTRA_SPICY"},
    {"InChef_Target_Field": "instructions", "IndianFoodDataset": "TranslatedInstructions", "DataHive": "ingredient_lines (or structured guide)", "CulinaryDB": "None", "Transformation_Rule": "Formatted numbered steps or paragraph text"},
    {"InChef_Target_Field": "image_url", "IndianFoodDataset": "NULL (or fallback)", "DataHive": "image_url", "CulinaryDB": "None", "Transformation_Rule": "Valid HTTP/HTTPS image URL or NULL"},
    {"InChef_Target_Field": "source_url", "IndianFoodDataset": "URL", "DataHive": "url", "CulinaryDB": "Source URL", "Transformation_Rule": "Original recipe link for attribution & provenance"},
    {"InChef_Target_Field": "source_dataset", "IndianFoodDataset": "'IndianFoodDataset'", "DataHive": "'DataHive'", "CulinaryDB": "'CulinaryDB'", "Transformation_Rule": "Provenance tracking"},
    {"InChef_Target_Field": "source_recipe_id", "IndianFoodDataset": "Srno", "DataHive": "URL hash / ID", "CulinaryDB": "Recipe ID", "Transformation_Rule": "Source tracking identifier"},
    {"InChef_Target_Field": "ingredient.name", "IndianFoodDataset": "TranslatedIngredients parsed", "DataHive": "ingredients[].food", "CulinaryDB": "Aliased Ingredient Name", "Transformation_Rule": "Canonicalized via CulinaryDB aliases + singularization"},
    {"InChef_Target_Field": "recipe_ingredient.quantity", "IndianFoodDataset": "Parsed quantity", "DataHive": "ingredients[].quantity", "CulinaryDB": "None", "Transformation_Rule": "Numeric double or 1.0 default"},
    {"InChef_Target_Field": "recipe_ingredient.unit", "IndianFoodDataset": "Parsed unit", "DataHive": "ingredients[].measure", "CulinaryDB": "None", "Transformation_Rule": "Mapped to MeasurementUnit enum"},
    {"InChef_Target_Field": "recipe_ingredient.notes", "IndianFoodDataset": "Parsed preparation note (e.g. chopped)", "DataHive": "ingredients[].text - food", "CulinaryDB": "None", "Transformation_Rule": "Preparation details text"}
]

csv_path = os.path.join(AUDIT_DIR, "field_mapping_preliminary.csv")
with open(csv_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["InChef_Target_Field", "IndianFoodDataset", "DataHive", "CulinaryDB", "Transformation_Rule"])
    writer.writeheader()
    writer.writerows(field_mapping)
print(f"Saved preliminary field mapping to {csv_path}")

# Write comprehensive dataset_audit_report.md
report_md = f"""# InChef Data Ingestion Pipeline — Dataset Audit Report

## 1. Executive Summary
This audit inspects the three raw source datasets provided for the InChef recipe recommendation platform:
1. **IndianFoodDatasetXLS.xlsx**: Primary source for authentic Indian and regional South Asian recipes with complete cooking times, ingredients, and instructions.
2. **CulinaryDB**: Ground-truth culinary knowledge base containing ingredient aliases, taxonomy categories, compound ingredients, and entity mappings.
3. **DataHive (recipes-with-nutrition)**: Primary expansion source for East Asian and Southeast Asian recipes, providing structured nutrient profiles, dietary/health labels, cautions, and image assets.

---

## 2. Indian Food Dataset Audit
- **Source File**: `IndianFoodDatasetXLS.xlsx`
- **Total Records**: {indian_total:,}
- **Duplicate Records**: {indian_dup_records}
- **Duplicate Recipe Names**: {indian_dup_names}
- **Columns**: {len(indian_cols)} (`{', '.join(df_indian.columns)}`)

### Column Availability & Quality
| Column | Dtype | Missing Count | Missing % |
|---|---|---|---|
"""
for col, cinfo in indian_cols.items():
    report_md += f"| `{col}` | {cinfo['dtype']} | {cinfo['missing_count']} | {cinfo['missing_pct']}% |\n"

report_md += f"""
### Key Metrics
- **Recipes with Ingredients**: {has_ingredients:,} ({has_ingredients/indian_total*100:.1f}%)
- **Recipes with Instructions**: {has_instructions:,} ({has_instructions/indian_total*100:.1f}%)
- **Recipes with Prep Time**: {has_prep:,} ({has_prep/indian_total*100:.1f}%)
- **Recipes with Cook Time**: {has_cook:,} ({has_cook/indian_total*100:.1f}%)
- **Recipes with Total Time**: {has_total:,} ({has_total/indian_total*100:.1f}%)
- **Recipes with Servings**: {has_servings:,} ({has_servings/indian_total*100:.1f}%)
- **Recipes with Source URL**: {has_url:,} ({has_url/indian_total*100:.1f}%)

### Top Cuisines in IndianFoodDataset
```json
{json.dumps(indian_cuisines, indent=2)}
```

### Top Courses
```json
{json.dumps(indian_courses, indent=2)}
```

### Top Diets
```json
{json.dumps(indian_diets, indent=2)}
```

---

## 3. CulinaryDB Audit
- **Directory**: `CulinaryDB/`
- **Role**: Taxonomic foundation for canonical ingredient resolution and alias normalization.

### File Details:
1. `01_Recipe_Details.csv`: **{c01_total:,}** recipes. Top cuisines: {json.dumps(c01_cuisines)}.
2. `02_Ingredients.csv`: **{c02_total:,}** canonical ingredient entities across categories: {json.dumps(culinary_stats['ingredients']['categories'])}.
3. `03_Compound_Ingredients.csv`: **{c03_total:,}** compound ingredients with constituent ingredient formulas (e.g. Garam Masala, Panch Phoron, Curry Powder).
4. `04_Recipe-Ingredients_Aliases.csv`: **{c04_total:,}** recipe-ingredient alias relationships covering **{culinary_stats['aliases']['unique_original_ingredients']:,}** original ingredient name variants mapped to **{culinary_stats['aliases']['unique_aliased_ingredients']:,}** aliased canonical names.

---

## 4. DataHive (recipes-with-nutrition) Audit
- **Source File**: `recipes-with-nutrition(DataHive).csv`
- **Total Records Analyzed**: {dh_total:,}
- **Unique Recipe Names**: {len(seen_names):,}
- **Duplicate Names**: {duplicate_name_count:,}
- **Columns**: {len(dh_cols)}

### Column Availability & Quality
| Column | Dtype | Missing Count | Missing % |
|---|---|---|---|
"""
for col, cinfo in dh_cols.items():
    report_md += f"| `{col}` | {cinfo['dtype']} | {cinfo['missing_count']} | {cinfo['missing_pct']}% |\n"

report_md += f"""
### Key Metrics
- **Recipes with Images**: {dh_metrics['has_image']:,} ({dh_metrics['has_image']/dh_total*100:.1f}%)
- **Recipes with Source URLs**: {dh_metrics['has_url']:,} ({dh_metrics['has_url']/dh_total*100:.1f}%)
- **Recipes with Servings**: {dh_metrics['has_servings']:,} ({dh_metrics['has_servings']/dh_total*100:.1f}%)
- **Recipes with Calories**: {dh_metrics['has_calories']:,} ({dh_metrics['has_calories']/dh_total*100:.1f}%)
- **Recipes with Diet Labels**: {dh_metrics['has_diet_labels']:,} ({dh_metrics['has_diet_labels']/dh_total*100:.1f}%)
- **Recipes with Health Labels**: {dh_metrics['has_health_labels']:,} ({dh_metrics['has_health_labels']/dh_total*100:.1f}%)
- **Recipes with Cautions**: {dh_metrics['has_cautions']:,} ({dh_metrics['has_cautions']/dh_total*100:.1f}%)
- **Recipes with Total Nutrients**: {dh_metrics['has_total_nutrients']:,} ({dh_metrics['has_total_nutrients']/dh_total*100:.1f}%)

### Cuisine Types in DataHive (Global distribution before Asian/Indian filtering)
```json
{json.dumps(stats['datahive_recipes']['cuisines_distribution'], indent=2)}
```

---

## 5. Synthesis & Filter Strategy for InChef
1. **Target Cuisine Focus**: InChef exclusively targets **Indian + South Asian + East Asian + Southeast Asian** cuisines.
   - All {indian_total:,} recipes from `IndianFoodDatasetXLS.xlsx` will be vetted and filtered for Indian / South Asian regional authenticity.
   - From `DataHive`, only recipes tagged with target Asian / Indian cuisines (e.g. `asian`, `indian`, `chinese`, `japanese`, `thai`, `korean`, `vietnamese`, `indonesian`, `filipino`, `malaysian`, `south east asian`) will be extracted, safely filtering out Western recipes (American, French, Nordic, etc.).
2. **Ingredient Canonicalization**: The 700K+ mappings from `CulinaryDB` will form our alias lookup graph to standardize variants (e.g., *scallion / green onion / spring onion* -> *green onion*) without unsafe substring collisions.
3. **Nutrition & Metadata Enrichment**: DataHive Asian recipes bring rich macro-nutrient data and dietary tags (Vegetarian, Vegan, Gluten-Free, Nut-Free) into the InChef model.
"""

report_path = os.path.join(AUDIT_DIR, "dataset_audit_report.md")
with open(report_path, "w", encoding="utf-8") as f:
    f.write(report_md)
print(f"Saved audit report to {report_path}")
