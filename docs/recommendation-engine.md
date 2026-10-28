# InChef — Intelligent Recipe Recommendation System: Technical Logic & Architecture

## Overview
InChef is an intelligent culinary platform supporting two strictly separated flows:
1. **Flow A: Direct Recipe Search** — Title/name based, case-insensitive, partial-match querying with deterministic ordering and pagination.
2. **Flow B: Ingredient-Based Recommendation** — Robust ingredient normalization, coverage calculation against a configurable threshold (≥ 75%), weighted multi-factor scoring, user personalization (dietary, cuisine, cooking time, and skill level), explainable recommendations, and pantry integration.

---

## 1. Direct Recipe Search (Flow A)
- **Endpoint**: `GET /api/v1/recipes/search?query={title}&page={page}&size={size}`
- **Behavior**:
  - Matches recipe titles using SQL `LIKE CONCAT('%', :query, '%')` (case-insensitive).
  - Never parses or tokenizes dishes into ingredients (e.g., searching `"Chicken Biryani"` searches for recipes with "Chicken Biryani" in the title; it does NOT treat it as `['Chicken', 'Biryani']`).
  - Safe `"all"` filter handling: When the UI filter is set to `"all"` or the search term is empty, the query returns all published recipes rather than filtering for the literal word "all".
  - Deterministic sort order: By relevance, descending creation date, and recipe ID.

---

## 2. Ingredient-Based Recommendation (Flow B)
- **Endpoint**: `POST /api/v1/recommendations/by-ingredients`
- **Request Body**:
```json
{
  "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
  "userId": 1,
  "usePantry": false,
  "dietaryType": "NON_VEGETARIAN",
  "cuisine": "Indian",
  "maxCookingTime": 45,
  "cookingSkillLevel": "INTERMEDIATE",
  "minIngredientMatch": 75.0,
  "limit": 10
}
```

### 2.1 Pipeline Flow
```
User Ingredients (+ Optional Pantry)
          ↓
Ingredient Normalization (singularize, lowercase, culinary alias mapping, word-boundary match)
          ↓
Ingredient Match Calculation (matchedRequiredIngredients / totalRecipeIngredients * 100)
          ↓
Configurable 75% Coverage Threshold (default 75.0%)
          ↓
Personalization & Safety Filters:
  ├── Safety: Allergy check (Dairy, Peanuts, Shellfish, Gluten, Eggs, Soy, etc. - NEVER bypassed)
  ├── Personalization: Dietary preference (VEGETARIAN, VEGAN, NON_VEGETARIAN, etc. - only if selected)
  ├── Personalization: Cuisine preference (INDIAN, ASIAN, ITALIAN, etc. - only if selected)
  ├── Personalization: Max cooking time (≤ limit minutes - only if selected)
  └── Personalization: Cooking skill level (BEGINNER, INTERMEDIATE, ADVANCED - only if selected)
          ↓
Deterministic Ranking & Scoring (Ingredient 60 + Dietary 15 + Cuisine 10 + Time 10 + MealType 5 + Skill)
          ↓
Explainable Response Cards with Score Breakdown, Matched & Missing Ingredients
```

---

## 3. 75% Ingredient Coverage Threshold
- **Formula**:
  $$\text{ingredientCoveragePercentage} = \frac{\text{matchedRequiredIngredients}}{\text{totalRecipeIngredients}} \times 100$$
- **Configurability**: Configurable via `recommendation.min-ingredient-match=${RECOMMENDATION_MIN_INGREDIENT_MATCH:75.0}` in `application.properties`.
- **Eligibility**:
  - Recipes with `ingredientCoveragePercentage >= 75.0%` are eligible for primary recommendation.
  - Recipes with coverage `< 75.0%` are excluded from the primary recommendation set.
  - If a user inputs empty ingredients (e.g., browsing all recommendations), the threshold check does not block candidate browsing.

---

## 4. Ingredient Normalization & Token Matching
- **Case & Whitespace Normalization**: All input tokens are trimmed and converted to lowercase.
- **Singularization**: Basic pluralization suffixes (`s`, `es`) are singularized.
- **Culinary Aliases & Synonyms**:
  - `scallion` / `scallions` / `spring onion` → `green onion`
  - `hari mirch` / `green chili` → `green chilli`
  - `dhania` / `cilantro` → `coriander`
  - `dahi` / `curd` → `yogurt`
  - `jeera` → `cumin`, `haldi` → `turmeric`, `adrak` → `ginger`, `lahsun` → `garlic`
  - `pyaaz` / `pyaz` → `onion`, `tamatar` → `tomato`
- **Word-Boundary Token Matching**:
  - Matches whole tokens and token prefixes/suffixes at word boundaries (`\b`).
  - Prevents unsafe substring matching:
    - `"pea"` does NOT match `"peanut"`
    - `"egg"` does NOT match `"eggplant"`
    - `"ice"` does NOT match `"rice"`
    - `"corn"` does NOT match `"peppercorn"`
  - Legitimate variations supported:
    - `"chicken"` matches `"chicken breast"`, `"chicken thigh"`
    - `"onion"` matches `"green onion"`, `"red onion"`

---

## 5. Match Scoring Architecture
InChef preserves the established weighted scoring model:

| Component | Max Points | Description |
| :--- | :--- | :--- |
| **Ingredient Coverage** | 60 | Proportional to matched required ingredients over total ingredients |
| **Dietary Compatibility** | 15 | Strict match with user's dietary restriction |
| **Cuisine Match** | 10 | Matches user's favorite / requested cuisine |
| **Cooking Time** | 10 | Proportional to recipes within user's target time |
| **Meal Type / Category** | 5 | Matches current meal time (Breakfast, Lunch, Dinner, Snack) |
| **Skill Compatibility** | 5 | Bonus for matching user's cooking skill level |
| **Total Score** | **100** | Clamped between 0 and 100 |

### Edge Case Handling
Recipes with very few ingredients do not unfairly dominate solely due to high percentage. Ties in match score are deterministically broken by:
1. Higher count of matched ingredients (e.g., matching 4 user ingredients beats matching 1)
2. Shorter cooking time
3. Stable recipe ID

---

## 6. Personalization Rules
1. **Unselected Preferences Do Not Filter**:
   - If no cuisine is selected → do NOT restrict cuisine.
   - If no dietary preference is selected → do NOT restrict dietary type.
   - If no cooking time limit is selected → do NOT restrict cooking time.
   - If no cooking skill is selected → do NOT restrict cooking skill.
2. **Cooking Skill Level Mapping**:
   - Enums: `CookingSkillLevel` (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`).
   - Mapped to existing recipe `DifficultyLevel`:
     - `BEGINNER`: Compatible with `EASY` recipes.
     - `INTERMEDIATE`: Compatible with `EASY` and `MEDIUM` recipes.
     - `ADVANCED`: Compatible with `EASY`, `MEDIUM`, and `HARD` recipes (with ranking preference for advanced recipes).
3. **Pantry Integration ("Use My Pantry")**:
   - Fetches saved user ingredients from `user_ingredients` table.
   - Merges pantry items with any manually supplied input tokens.
   - Feeds the combined set into the recommendation pipeline seamlessly.

---

## 7. Safety & Allergy Rules
- **Non-Bypassable Constraint**: Allergy restrictions (Dairy, Peanuts, Shellfish, Gluten, Eggs, Soy, etc.) are **never bypassed**, even if ingredient match coverage is 100%.
- If a recipe contains an allergen listed in `user_preferences.allergies`, it is strictly excluded from recommendations.

---

## 8. Database Schema & Preservation
- **Existing Records Preserved**:
  - `recipes`: 8,061 rows (no duplicates created; difficulty column utilized).
  - `ingredients`: 4,871 canonical ingredients preserved.
  - `recipe_ingredients`: 90,833 relationships preserved.
  - `users`: 3 users preserved.
  - `user_preferences`: Added `cooking_skill_level` column (`VARCHAR(32)`).
  - `user_ingredients`: 22 pantry items preserved.
  - `favorites`: 6 favorite records preserved.
  - `recommendation_logs`: Preserved and actively logging each recommendation request.
- **Indexes**:
  - `recipes.title`, `recipes.cuisine`, `recipes.dietary_type`, `recipes.difficulty`
  - `recipe_ingredients.recipe_id`, `recipe_ingredients.ingredient_id`

---

## 9. API Endpoints Reference
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/recipes/search?query={q}&page=0&size=12` | Direct Recipe Search |
| `POST` | `/api/v1/recommendations/by-ingredients` | Ingredient-Based Recommendations (≥75% coverage) |
| `POST` | `/api/v1/recommendations/by-dish` | Dish-based recommendation with recipe lookup |
| `GET` | `/api/v1/recipes/{id}` | Full Recipe Details (ingredients + steps + nutrition) |
| `GET` | `/api/v1/user-preferences/{userId}` | Get User Preferences |
| `PUT` | `/api/v1/user-preferences/{userId}` | Save User Preferences (diet, cuisine, time, skill) |
| `GET` | `/api/v1/users/{userId}/ingredients` | Get User Pantry Items |
| `POST` | `/api/v1/users/{userId}/ingredients` | Add User Pantry Item |
