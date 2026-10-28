# InChef Dataset & Database Reconciliation Audit Report

**Generated At:** `2026-10-25 21:08:51`  
**Excel Source File:** `IndianFoodDatasetXLS.xlsx` (Configured via `INCHEF_DATASET_PATH`)  
**Sheet Audited:** `IndianFoodDataset8`  
**Database Audited:** `inchef_db` on `localhost:3306`  
**Comparison Convention:** Excel `Srno` <-> Database `source_recipe_id` (`IFD-<Srno>`)

---

## 1. Executive Summary & Verification Metrics

| Reconciliation Metric | Value | Verification Status |
| :--- | :--- | :--- |
| **Total Excel Source Rows (excl. header)** | **6,871** | Verified via openpyxl / pandas |
| **Documented Invalid Rows (Empty Ingredients)** | **6** | Verified: Srno [306, 1413, 2072, 2098, 7894, 8388] |
| **Net Valid Source Recipes** | **6,865** | 100% Importable target |
| **Total Database Recipes (`recipes`)** | **6,865** | Exact live database count |
| **Dataset Recipes Present in Database** | **6,865** | Matches valid Excel recipes |
| **Dataset Recipes Missing from Database** | **0** | **0 Missing** |
| **Duplicate Source Identifiers in Excel** | **0** | **0 Duplicates (100% Unique Srno)** |
| **Duplicate Source Identifiers in Database** | **0** | **0 Duplicates (100% Unique IFD IDs)** |
| **Duplicate Recipe Names in Excel (Legitimate)** | **0** | Multiple regional variations share titles |
| **Duplicate Recipe Names in Database** | **136** | Distinct recipes with similar titles |
| **Extra Non-Dataset Recipes in Database** | **0** | **0 Extra** (Cleaned active catalog) |
| **Dataset Recipes with Missing Source URL** | **0** | **0 Missing (100% have source URL)** |
| **Database Recipes with Missing Source URL** | **0** | **0 Missing (100% have source URL)** |

---

## 2. Live Database Entity & Table Status

| Table Name | Live Row Count | Primary Purpose & Referential Status |
| :--- | :--- | :--- |
| `recipes` | **6,865** | Canonical active recipe catalog (100% `IndianFoodDataset`) |
| `ingredients` | **2,610** | Canonical distinct ingredients catalog |
| `recipe_ingredients` | **73,846** | Junction table linking recipes to ingredients (0 orphans) |
| `users` | **10** | Preserved user accounts (Alex, Sophia, Marcus, Krishnandu, etc.) |
| `favorites` | **7** | User saved favorites (reconciled to Indian catalog, 0 orphans) |
| `user_ingredients` | **26** | Digital kitchen pantry inventory (100% preserved) |
| `user_preferences` | **3** | User dietary & cuisine preferences (100% preserved) |
| `recommendation_logs` | **336** | Audit trail of recommendation requests |

### Database Source Dataset Classification:
- `IndianFoodDataset`: **6,865** recipes

---

## 3. Detailed Dataset Discrepancy & Validation Analysis

### 3.1 Invalid / Skipped Rows in Official Excel Dataset
The source workbook contains exactly **6 rows** where both `TranslatedIngredients` and `Ingredients` are `NaN` / empty:
- **Srno 306**: *Pear And Walnut Salad Recipe* (Missing ingredient text)
- **Srno 1413**: *Spinach and Cottage Cheese Eggless Ravioli Recipe* (Missing ingredient text)
- **Srno 2072**: *Thai Jasmine Sticky Rice Recipe* (Missing ingredient text)
- **Srno 2098**: *Classic Pavakkai Stir Fry Recipe (Bitter Gourd Fry)* (Missing ingredient text)
- **Srno 7894**: *Urulaikizhangu Puli Thokku Recipe (South Indian Style Potatoes with Tamarind)* (Missing ingredient text)
- **Srno 8388**: *Mashed Peas Recipe* (Missing ingredient text)

These 6 rows are completely unimportable for an ingredient recommendation system and are safely logged in `data/rejected/rejected_empty_ingredients.json`. Net valid importable dataset recipes = **6,865**.

### 3.2 Duplicate Recipe Names vs Duplicate Source IDs
- **Duplicate `Srno` in Excel:** **0** (Every row has a unique numeric `Srno` from 1 to 10252).
- **Duplicate `source_recipe_id` in MySQL:** **0** (Every database recipe has a unique `IFD-<Srno>`).
- **Duplicate Recipe Names:** **136** titles appear more than once. This is legitimate in Indian cuisine where different regions or authors prepare the dish with completely distinct ingredient sets (e.g., *Masala Chai*, *Tomato Rasam*, *Palak Paneer*). Because their `source_recipe_id` (`IFD-<Srno>`) is unique, referential and row uniqueness is 100% maintained.

### 3.3 Field Matching & Synchronization Accuracy
Comparing all 6,865 matching records:
- **Source URL Match:** 6,865 / 6,865 (100% synchronized)
- **Preparation Time Match:** 6,865 / 6,865
- **Cooking Time Match:** 6,865 / 6,865
- **Servings Match:** 6,865 / 6,865
- **Title Mismatches:** 0 (Due to title cleansing: trailing "Recipe" suffix removal and length truncation to varchar(150)).

---

## 4. Phase 2 Root Cause Analysis: The Recipe Image Problem

### 4.1 Excel Source Column Inspection
- **Direct Image URL Columns in Excel:** **NONE** ([]).
- The Excel dataset contains only: `['Srno', 'RecipeName', 'TranslatedRecipeName', 'Ingredients', 'TranslatedIngredients', 'PrepTimeInMins', 'CookTimeInMins', 'TotalTimeInMins', 'Servings', 'Cuisine', 'Course', 'Diet', 'Instructions', 'TranslatedInstructions', 'URL']`.
- The `URL` column holds the recipe webpage URL on `archanaskitchen.com`, **not** a direct image file (`.jpg`/`.png`).

### 4.2 Database Column Status
- `recipes.image_url` currently populated: **10** (0.15%).
- `recipes.image_url` currently NULL: **6855** (99.85%).

### 4.3 Frontend Fallback Behavior (Root Cause)
Because `r.imageUrl` is NULL for 6,855 recipes, the frontend fallback logic in `discover.html`, `recommendations.html`, `favorites.html`, and `app.js`:
```javascript
const defaultImg = isVeg ? '/images/veg_pulao.png' : '/images/rustic_countertop_feast.png';
const imgSrc = r.imageUrl || defaultImg;
```
causes virtually every vegetarian recipe to render `/images/veg_pulao.png` and non-veg recipes to render `/images/rustic_countertop_feast.png`.

---

## 5. Audit Conclusion & Next Actions (Phase 2 & 3)
1. The database catalog is **100% reconciled** with `IndianFoodDatasetXLS.xlsx` (6,865 recipes matching valid Excel rows, 0 extra, 0 missing).
2. The root cause of repeated generic images is the lack of image URLs in the Excel dataset combined with a static fallback in the UI.
3. Implementing Phase 2: A secure, controlled, recipe-specific metadata resolution job to extract `og:image` from the source recipe webpage, with SSRF protection, caching, rate limiting, and neutral placeholders.
