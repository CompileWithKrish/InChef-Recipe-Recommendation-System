# IndianFoodDataset Import Summary Report

**Date:** 2026-10-09 22:48:08  
**Source File:** `IndianFoodDatasetXLS.xlsx` (Configured via `INCHEF_DATASET_PATH`)  
**Target Database:** `inchef_db` on `localhost:3306`  

## Execution Statistics

| Metric | Value |
| :--- | :--- |
| **Total Source Rows** | 6871 |
| **Successfully Imported Recipes** | 2031 |
| **Updated Existing Recipes** | 4834 |
| **Skipped Duplicates** | 0 |
| **Rejected Rows** | 6 |
| **Missing Ingredients Rows** | 6 |
| **Missing Instructions Rows** | 0 |
| **New Canonical Ingredients Created** | 1408 |
| **Recipe-Ingredient Links Synced** | 74450 |
| **Validation Errors** | 6 |

## Integrity Verification
- **Traceability:** Recipes retain `source_recipe_id` (`Srno`), `source_dataset` (`IndianFoodDataset`), `source_url`, `translated_recipe_name`, and `ingredient_text`.
- **Idempotency:** Re-running the pipeline updates matching `source_recipe_id` rows without duplicating recipes or recipe-ingredient links.
- **User Safety:** 0 user accounts, favorites, pantry items, or user preferences were deleted or modified.
