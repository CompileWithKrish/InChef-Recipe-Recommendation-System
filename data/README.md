# InChef — Data Directory & Dataset Management

This directory contains datasets, preprocessed tables, validation audits, migration scripts, and safe samples used by the InChef recommendation engine and database importer.

---

## 1. Directory Structure

```text
data/
├── README.md                          # This document
├── LICENSES.md                        # Dataset licensing and attribution
├── samples/
│   └── sample_recipes.xlsx            # 10-row sample of the Indian Food Dataset
├── processed/                         # Canonical relational table CSVs for MySQL ingestion
│   ├── inchef_ingredients.csv         # Direct relational table CSV for MySQL
│   ├── inchef_recipes.csv             # Direct relational table CSV for MySQL
│   └── inchef_recipe_ingredients.csv  # Direct relational table CSV for MySQL
├── rejected/
│   ├── rejected_empty_ingredients.json
│   ├── rejected_ingredients.csv
│   └── rejected_recipes.csv
├── audit/
│   ├── dataset_audit_report.md        # Comprehensive dataset profiling report
│   ├── dataset_statistics.json        # Ingredient frequency & distribution metrics
│   └── query_test_results.json        # Test query benchmarks
├── reports/
│   ├── data_quality_report.md
│   ├── final_data_report.md
│   └── migration_summary_report.md
└── migrations/
    ├── V2__add_recipe_image_metadata.sql # Recipe image columns & indices
    └── V3__add_login_history.sql         # Login history audit table
```

> [!NOTE]
> Database dumps and local backup files (e.g. `data/backup/*.sql`) are intentionally ignored by `.gitignore` to prevent tracking large binary/SQL dumps in version control.

---

## 2. Dataset Schema Reference

The primary dataset is derived from the **Indian Food Dataset** (`IndianFoodDatasetXLS.xlsx`), containing 15 tabular columns:

| Column Header | Type | Description |
| :--- | :--- | :--- |
| `Srno` | Integer | Original dataset serial identifier |
| `RecipeName` | Text | Native dish title |
| `TranslatedRecipeName` | Text | English translated dish title |
| `Ingredients` | Text | Raw ingredient list with vernacular names |
| `TranslatedIngredients` | Text | English ingredient list with quantities and units |
| `PrepTimeInMins` | Integer | Preparation duration in minutes |
| `CookTimeInMins` | Integer | Cooking duration in minutes |
| `TotalTimeInMins` | Integer | Total elapsed preparation + cook duration |
| `Servings` | Integer | Recommended serving size count |
| `Cuisine` | Text | Regional Indian or continental cuisine category |
| `Course` | Text | Meal course (Breakfast, Lunch, Dinner, Snack, Dessert, etc.) |
| `Diet` | Text | Dietary tag (Vegetarian, Non-Vegetarian, Vegan, Diabetic Friendly) |
| `Instructions` | Text | Native language step-by-step preparation instructions |
| `TranslatedInstructions` | Text | English translated step-by-step cooking steps |
| `URL` | URL | Canonical source URL for recipe provenance and image scraping |

---

## 3. Sample Dataset (`data/samples/sample_recipes.xlsx`)

For CI/CD testing, local environment setup, and verification without the full 6,000+ row dataset, a verified 10-recipe sample is provided in `data/samples/sample_recipes.xlsx`.

To test dataset importing using this sample:

```bash
python scripts/import_dataset.py --dataset data/samples/sample_recipes.xlsx --dry-run
```

To import the sample into your local MySQL development database:

```bash
python scripts/import_dataset.py --dataset data/samples/sample_recipes.xlsx
```

---

## 4. Full Dataset Import Instructions

To import the complete dataset into MySQL:

1. Ensure the MySQL database `inchef_db` is running (`docker compose up -d` or native MySQL service).
2. Set the dataset path via environment variable or command-line flag:
   ```bash
   # Option A: Environment variable
   export INCHEF_DATASET_PATH="/path/to/IndianFoodDatasetXLS.xlsx"
   python scripts/import_dataset.py

   # Option B: Direct CLI parameter
   python scripts/import_dataset.py --dataset "/path/to/IndianFoodDatasetXLS.xlsx"
   ```
3. Use `--dry-run` to preview insertions and validations without writing changes:
   ```bash
   python scripts/import_dataset.py --dry-run
   ```

