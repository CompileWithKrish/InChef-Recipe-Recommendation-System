# InChef Data Ingestion Pipeline — Dataset Audit Report

## 1. Executive Summary
This audit inspects the three raw source datasets provided for the InChef recipe recommendation platform:
1. **IndianFoodDatasetXLS.xlsx**: Primary source for authentic Indian and regional South Asian recipes with complete cooking times, ingredients, and instructions.
2. **CulinaryDB**: Ground-truth culinary knowledge base containing ingredient aliases, taxonomy categories, compound ingredients, and entity mappings.
3. **DataHive (recipes-with-nutrition)**: Primary expansion source for East Asian and Southeast Asian recipes, providing structured nutrient profiles, dietary/health labels, cautions, and image assets.

---

## 2. Indian Food Dataset Audit
- **Source File**: `IndianFoodDatasetXLS.xlsx`
- **Total Records**: 6,871
- **Duplicate Records**: 0
- **Duplicate Recipe Names**: 136
- **Columns**: 15 (`Srno, RecipeName, TranslatedRecipeName, Ingredients, TranslatedIngredients, PrepTimeInMins, CookTimeInMins, TotalTimeInMins, Servings, Cuisine, Course, Diet, Instructions, TranslatedInstructions, URL`)

### Column Availability & Quality
| Column | Dtype | Missing Count | Missing % |
|---|---|---|---|
| `Srno` | int64 | 0 | 0.0% |
| `RecipeName` | object | 0 | 0.0% |
| `TranslatedRecipeName` | object | 0 | 0.0% |
| `Ingredients` | object | 6 | 0.09% |
| `TranslatedIngredients` | object | 6 | 0.09% |
| `PrepTimeInMins` | int64 | 0 | 0.0% |
| `CookTimeInMins` | int64 | 0 | 0.0% |
| `TotalTimeInMins` | int64 | 0 | 0.0% |
| `Servings` | int64 | 0 | 0.0% |
| `Cuisine` | object | 0 | 0.0% |
| `Course` | object | 0 | 0.0% |
| `Diet` | object | 0 | 0.0% |
| `Instructions` | object | 0 | 0.0% |
| `TranslatedInstructions` | object | 0 | 0.0% |
| `URL` | object | 0 | 0.0% |

### Key Metrics
- **Recipes with Ingredients**: 6,865 (99.9%)
- **Recipes with Instructions**: 6,871 (100.0%)
- **Recipes with Prep Time**: 6,744 (98.2%)
- **Recipes with Cook Time**: 6,636 (96.6%)
- **Recipes with Total Time**: 6,868 (100.0%)
- **Recipes with Servings**: 6,871 (100.0%)
- **Recipes with Source URL**: 6,871 (100.0%)

### Top Cuisines in IndianFoodDataset
```json
{
  "Indian": 1157,
  "Continental": 1021,
  "North Indian Recipes": 938,
  "South Indian Recipes": 682,
  "Italian Recipes": 236,
  "Bengali Recipes": 175,
  "Maharashtrian Recipes": 173,
  "Kerala Recipes": 163,
  "Tamil Nadu": 156,
  "Karnataka": 149,
  "Fusion": 135,
  "Rajasthani": 123,
  "Mexican": 119,
  "Andhra": 118,
  "Gujarati Recipes\ufeff": 115,
  "Goan Recipes": 99,
  "Punjabi": 84,
  "Chettinad": 74,
  "Asian": 72,
  "Thai": 66
}
```

### Top Courses
```json
{
  "Lunch": 1765,
  "Side Dish": 992,
  "Snack": 876,
  "Dinner": 782,
  "Dessert": 659,
  "Appetizer": 639,
  "Main Course": 315,
  "South Indian Breakfast": 260,
  "World Breakfast": 260,
  "North Indian Breakfast": 123,
  "Indian Breakfast": 101,
  "Vegetarian": 47,
  "One Pot Dish": 33,
  "High Protein Vegetarian": 7,
  "Brunch": 4,
  "Vegan": 3,
  "Non Vegeterian": 2,
  "Eggetarian": 1,
  "No Onion No Garlic (Sattvic)": 1,
  "Sugar Free Diet": 1
}
```

### Top Diets
```json
{
  "Vegetarian": 4712,
  "High Protein Vegetarian": 705,
  "Non Vegeterian": 427,
  "Eggetarian": 344,
  "Diabetic Friendly": 260,
  "High Protein Non Vegetarian": 225,
  "No Onion No Garlic (Sattvic)": 73,
  "Vegan": 61,
  "Gluten Free": 50,
  "Sugar Free Diet": 14
}
```

---

## 3. CulinaryDB Audit
- **Directory**: `CulinaryDB/`
- **Role**: Taxonomic foundation for canonical ingredient resolution and alias normalization.

### File Details:
1. `01_Recipe_Details.csv`: **45,772** recipes. Top cuisines: {"USA": 16118, "Italy": 7504, "Indian Subcontinent": 4058, "Mexico": 3138, "France": 2703, "Canada": 1112, "Caribbean": 1103, "British Isles": 1075, "Middle East": 993, "China": 941, "Greece": 934, "Spain": 816, "Thailand": 667, "Africa": 651, "South East Asia": 611}.
2. `02_Ingredients.csv`: **930** canonical ingredient entities across categories: {"Fruit": 136, "Fish": 119, "Vegetable": 73, "Dish": 68, "Plant": 64, "Herb": 51, "Meat": 49, "Dairy": 45, "Beverage Alcoholic": 45, "Essential Oil": 42, "Seafood": 35, "Bakery": 30, "Additive": 28, "Spice": 26, "Nuts & Seed": 25, "Legume": 23, "Cereal": 23, "Beverage": 21, "Fungus": 12, "Flower": 9, "Maize": 6}.
3. `03_Compound_Ingredients.csv`: **103** compound ingredients with constituent ingredient formulas (e.g. Garam Masala, Panch Phoron, Curry Powder).
4. `04_Recipe-Ingredients_Aliases.csv`: **456,279** recipe-ingredient alias relationships covering **161,208** original ingredient name variants mapped to **878** aliased canonical names.

---

## 4. DataHive (recipes-with-nutrition) Audit
- **Source File**: `recipes-with-nutrition(DataHive).csv`
- **Total Records Analyzed**: 39,447
- **Unique Recipe Names**: 38,103
- **Duplicate Names**: 1,344
- **Columns**: 18

### Column Availability & Quality
| Column | Dtype | Missing Count | Missing % |
|---|---|---|---|
| `recipe_name` | object | 0 | 0.0% |
| `source` | object | 0 | 0.0% |
| `url` | object | 0 | 0.0% |
| `servings` | float64 | 0 | 0.0% |
| `calories` | float64 | 0 | 0.0% |
| `total_weight_g` | float64 | 0 | 0.0% |
| `image_url` | object | 277 | 0.7% |
| `diet_labels` | object | 0 | 0.0% |
| `health_labels` | object | 0 | 0.0% |
| `cautions` | object | 0 | 0.0% |
| `cuisine_type` | object | 0 | 0.0% |
| `meal_type` | object | 0 | 0.0% |
| `dish_type` | object | 0 | 0.0% |
| `ingredient_lines` | object | 0 | 0.0% |
| `ingredients` | object | 0 | 0.0% |
| `total_nutrients` | object | 0 | 0.0% |
| `daily_values` | object | 0 | 0.0% |
| `digest` | object | 0 | 0.0% |

### Key Metrics
- **Recipes with Images**: 39,170 (99.3%)
- **Recipes with Source URLs**: 39,447 (100.0%)
- **Recipes with Servings**: 39,447 (100.0%)
- **Recipes with Calories**: 39,447 (100.0%)
- **Recipes with Diet Labels**: 33,649 (85.3%)
- **Recipes with Health Labels**: 39,234 (99.5%)
- **Recipes with Cautions**: 32,795 (83.1%)
- **Recipes with Total Nutrients**: 39,447 (100.0%)

### Cuisine Types in DataHive (Global distribution before Asian/Indian filtering)
```json
{
  "american": 16879,
  "italian": 4480,
  "mediterranean": 3785,
  "french": 3111,
  "british": 1667,
  "mexican": 1657,
  "world": 1310,
  "south american": 1195,
  "indian": 915,
  "chinese": 786,
  "nordic": 772,
  "middle eastern": 737,
  "south east asian": 702,
  "asian": 598,
  "central europe": 456,
  "japanese": 394,
  "eastern europe": 292,
  "caribbean": 265,
  "greek": 236,
  "korean": 84,
  "kosher": 44
}
```

---

## 5. Synthesis & Filter Strategy for InChef
1. **Target Cuisine Focus**: InChef exclusively targets **Indian + South Asian + East Asian + Southeast Asian** cuisines.
   - All 6,871 recipes from `IndianFoodDatasetXLS.xlsx` will be vetted and filtered for Indian / South Asian regional authenticity.
   - From `DataHive`, only recipes tagged with target Asian / Indian cuisines (e.g. `asian`, `indian`, `chinese`, `japanese`, `thai`, `korean`, `vietnamese`, `indonesian`, `filipino`, `malaysian`, `south east asian`) will be extracted, safely filtering out Western recipes (American, French, Nordic, etc.).
2. **Ingredient Canonicalization**: The 700K+ mappings from `CulinaryDB` will form our alias lookup graph to standardize variants (e.g., *scallion / green onion / spring onion* -> *green onion*) without unsafe substring collisions.
3. **Nutrition & Metadata Enrichment**: DataHive Asian recipes bring rich macro-nutrient data and dietary tags (Vegetarian, Vegan, Gluten-Free, Nut-Free) into the InChef model.
