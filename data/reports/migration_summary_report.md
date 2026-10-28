# InChef Official Dataset Migration & Database Purge Report

**Date & Time:** 2026-10-25 20:10:41  
**Source Dataset:** `IndianFoodDatasetXLS.xlsx` (Sheet: `IndianFoodDataset8`, Rows: 6871, Configured via `INCHEF_DATASET_PATH`)  
**Pre-migration Backup:** `data/backup/inchef_db_backup_pre_migration.sql` (56.13 MB, local gitignored backup)  

---

## 1. Migration Summary

All obsolete records (legacy DataHive recipes and showcase records) have been successfully removed in strict foreign-key order. The active recipe catalog is now 100% powered by the official **IndianFoodDataset** (6865 valid recipes).

| Entity / Metric | Count | Status |
| :--- | :--- | :--- |
| **Excel Source Rows** | 6871 | 100% Processed |
| **Invalid Rows Skipped (Empty Ingredients)** | 6 | Logged in `data/rejected/rejected_empty_ingredients.json` |
| **Active Recipe Catalog (`recipes`)** | **6865** | **100% IndianFoodDataset** |
| **Obsolete Recipes Purged** | **3227** | **0 Remaining** |
| **Distinct Canonical Ingredients** | **2610** | Clean, dataset-driven |
| **Recipe-Ingredient Junction Links** | **73846** | 0 Orphans |
| **Preserved User Accounts** | **10** | 100% Intact |
| **Preserved User Pantry Items** | **26** | 100% Intact |
| **Preserved User Favorites** | **7** | Reconciled to Indian recipes |

---

## 2. Integrity & Referential Safety
- **No Cascade Loss:** 0 user accounts, preferences, or passwords were lost.
- **Idempotency:** Stable identifier `IFD-<Srno>` allows rerun without duplicate insertions.
- **Referential Integrity:** 0 broken foreign key references.
