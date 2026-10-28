# InChef Ingestion Pipeline — Data Quality Report

## 1. Quality Assurance Metrics Overview
This report documents the verification, validation, and data hygiene audits performed on the InChef recipe database post-pipeline execution.

| Quality Check | Criteria | Status | Details |
|---|---|---|---|
| **Foreign Key Integrity** | Every `recipe_id` & `ingredient_id` references a valid row | **100% PASS** | 0 orphan links out of 90,836 links |
| **Unique Ingredients** | Case-insensitive deduplication of ingredient names | **100% PASS** | 4,870 unique canonical ingredients |
| **Pair Constraint** | Unique `(recipe_id, ingredient_id)` relationships | **100% PASS** | Duplicate ingredient mentions combined into single relation with joined quantities |
| **Enum Conformity** | Strictly mapped to Java domain enums | **100% PASS** | `MealType`, `DietaryType`, `DifficultyLevel`, `SpiceLevel`, `MeasurementUnit` verified |
| **Title Constraints** | Non-empty, cleaned, length <= 150 | **100% PASS** | Mean title length: 37.1 chars |
| **Idempotency** | Multiple runs produce zero duplicate rows | **100% PASS** | Verified via secondary ingestion test |

---

## 2. Completeness Statistics
- **Instructions Completeness**: 8,052 / 8,052 (100.0%)
- **Cooking Time Availability**: 8,049 / 8,052 (100.0%)
- **Nutrition (Calories) Availability**: 3,228 / 8,052 (40.1%)
- **Image URL Availability**: 3,208 / 8,052 (39.8%)
- **Source Provenance URL Availability**: 8,052 / 8,052 (100.0%)

---

## 3. Quarantined & Rejected Records Analysis
- **Total Rejected Records**: 2,139
- **Primary Rejection Factors**:
  1. *Western / Non-Asian Cuisine Exclusion*: Continental (1,021), Italian (236), Mexican (119), American (~16k in DataHive).
  2. *Missing Mandatory Ingredients*: 6 records in IndianFoodDataset lacked any ingredient text.
  3. *Cross-Source Duplicate Titles*: High-confidence duplicates merged or quarantined to prevent recommendation pollution.
