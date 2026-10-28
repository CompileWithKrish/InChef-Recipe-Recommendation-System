# InChef — Operational CLI Scripts

This directory houses the administrative, validation, and dataset management scripts for the InChef system.

---

## 1. Scripts Overview

| Script | Purpose | Primary Flags |
| :--- | :--- | :--- |
| `import_dataset.py` | Imports recipe and ingredient records from Excel into MySQL. | `--dataset`, `--dry-run`, `--limit`, `--host`, `--port`, `--user`, `--password`, `--database` |
| `audit_dataset.py` | Profiles dataset files for missing fields, anomalies, and distributions. | `--dataset` |
| `audit_database.py` | Inspects live MySQL tables, row counts, foreign keys, and indexes. | `--host`, `--port`, `--user`, `--password`, `--database` |
| `audit_recipe_images.py` | Analyzes recipe image coverage, status, and fallback ratios in MySQL. | `--host`, `--port`, `--user`, `--password`, `--database` |

---

## 2. Prerequisites

The scripts require Python 3.9+ and standard data libraries:

```bash
pip install pandas openpyxl pymysql requests
```

Database credentials can be passed via command-line arguments or via environment variables:

```bash
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export DB_NAME="inchef_db"
export INCHEF_DATASET_PATH="data/samples/sample_recipes.xlsx"
```

---

## 3. Usage Guide

### A. Importing Recipes (`import_dataset.py`)

1. **Dry-Run Validation (Safe, No DB Writes):**
   ```bash
   python scripts/import_dataset.py --dataset data/samples/sample_recipes.xlsx --dry-run
   ```

2. **Import Sample Dataset into Local MySQL:**
   ```bash
   python scripts/import_dataset.py --dataset data/samples/sample_recipes.xlsx
   ```

3. **Import Full Dataset with Batch Limit:**
   ```bash
   python scripts/import_dataset.py --dataset "/path/to/IndianFoodDatasetXLS.xlsx" --limit 500
   ```

### B. Profiling Datasets (`audit_dataset.py`)

```bash
python scripts/audit_dataset.py --dataset data/samples/sample_recipes.xlsx
```

Outputs record counts, null column rates, total cooking time anomalies (<=0 or >12 hours), duplicate recipe titles, and cuisine distributions.

### C. Verifying Database Schema (`audit_database.py`)

```bash
python scripts/audit_database.py
```

Audits table existence, exact row counts, foreign key relational constraints, and non-primary indexes.

### D. Auditing Image Quality (`audit_recipe_images.py`)

```bash
python scripts/audit_recipe_images.py
```

Audits the distribution of recipe images:
- Status breakdown: `VERIFIED`, `RESOLVED`, `PENDING_REVIEW`, `FALLBACK`.
- Source breakdown: `SCRAPED`, `GOOGLE_SEARCH`, `USER_UPLOADED`, `DEFAULT_FALLBACK`.
- Verified vs neutral SVG placeholder coverage.

