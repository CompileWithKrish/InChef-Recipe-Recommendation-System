import os
import pandas as pd

PROCESSED_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "processed")

VALID_UNITS = {"GRAMS", "KILOGRAMS", "MILLILITERS", "LITERS", "CUPS", "TABLESPOONS", "TEASPOONS", "PIECES", "PINCH"}
VALID_MEAL_TYPES = {"BREAKFAST", "LUNCH", "DINNER", "SNACK", "DESSERT", "BEVERAGE"}
VALID_DIETARY_TYPES = {"VEGETARIAN", "VEGAN", "NON_VEGETARIAN", "EGGETARIAN", "GLUTEN_FREE", "KETO", "DAIRY_FREE"}
VALID_DIFFICULTY = {"EASY", "MEDIUM", "HARD"}
VALID_SPICE = {"MILD", "MEDIUM", "SPICY", "EXTRA_SPICY"}

def validate():
    print("=== Validating Processed InChef Datasets ===")
    df_recipes = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_recipes.csv"))
    df_ingredients = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_ingredients.csv"))
    df_ri = pd.read_csv(os.path.join(PROCESSED_DIR, "inchef_recipe_ingredients.csv"))

    errors = []

    # 1. Recipes checks
    print(f"Checking {len(df_recipes)} recipes...")
    if df_recipes['recipe_id'].duplicated().any():
        errors.append("Duplicate recipe_id found in inchef_recipes.csv")
    if df_recipes['title'].isna().any():
        errors.append("Null title found in inchef_recipes.csv")
    
    # Enum checks
    invalid_meals = set(df_recipes['meal_type'].dropna()) - VALID_MEAL_TYPES
    if invalid_meals:
        errors.append(f"Invalid meal_type: {invalid_meals}")

    invalid_diets = set(df_recipes['dietary_type'].dropna()) - VALID_DIETARY_TYPES
    if invalid_diets:
        errors.append(f"Invalid dietary_type: {invalid_diets}")

    invalid_diff = set(df_recipes['difficulty'].dropna()) - VALID_DIFFICULTY
    if invalid_diff:
        errors.append(f"Invalid difficulty: {invalid_diff}")

    invalid_spice = set(df_recipes['spice_level'].dropna()) - VALID_SPICE
    if invalid_spice:
        errors.append(f"Invalid spice_level: {invalid_spice}")

    # Check title lengths
    long_titles = df_recipes[df_recipes['title'].str.len() > 150]
    if len(long_titles) > 0:
        errors.append(f"Found {len(long_titles)} titles longer than 150 chars")

    # 2. Ingredients checks
    print(f"Checking {len(df_ingredients)} ingredients...")
    if df_ingredients['id'].duplicated().any():
        errors.append("Duplicate ingredient id found in inchef_ingredients.csv")
    
    names_lower = df_ingredients['name'].str.lower()
    if names_lower.duplicated().any():
        dup_cnt = names_lower.duplicated().sum()
        errors.append(f"Duplicate ingredient names found (case-insensitive count: {dup_cnt})")

    if df_ingredients['category'].isna().any():
        errors.append("Null category found in inchef_ingredients.csv")

    long_names = df_ingredients[df_ingredients['name'].str.len() > 100]
    if len(long_names) > 0:
        errors.append(f"Found {len(long_names)} ingredient names longer than 100 chars")

    # 3. Recipe-Ingredients FK checks
    print(f"Checking {len(df_ri)} recipe-ingredient links...")
    recipe_id_set = set(df_recipes['recipe_id'])
    ingredient_id_set = set(df_ingredients['id'])

    orphan_recipes = set(df_ri['recipe_id']) - recipe_id_set
    if orphan_recipes:
        errors.append(f"Found {len(orphan_recipes)} orphan recipe_id references in recipe_ingredients")

    orphan_ingredients = set(df_ri['ingredient_id']) - ingredient_id_set
    if orphan_ingredients:
        errors.append(f"Found {len(orphan_ingredients)} orphan ingredient_id references in recipe_ingredients")

    invalid_units = set(df_ri['unit'].dropna()) - VALID_UNITS
    if invalid_units:
        errors.append(f"Invalid unit found: {invalid_units}")

    # Non-null quantity & unit check
    if df_ri['quantity'].isna().any() or (df_ri['quantity'] <= 0).any():
        errors.append("Found null or non-positive quantity in recipe_ingredients")
    if df_ri['unit'].isna().any():
        errors.append("Found null unit in recipe_ingredients")

    # Duplicate constraint (recipe_id, ingredient_id)
    dup_pairs = df_ri.duplicated(subset=['recipe_id', 'ingredient_id']).sum()
    if dup_pairs > 0:
        print(f"Notice: {dup_pairs} duplicate (recipe_id, ingredient_id) pairs found. Will deduplicate!")
        df_ri = df_ri.drop_duplicates(subset=['recipe_id', 'ingredient_id'])
        df_ri.to_csv(os.path.join(PROCESSED_DIR, "inchef_recipe_ingredients.csv"), index=False)
        print(f"Cleaned up duplicates in inchef_recipe_ingredients.csv. Remaining: {len(df_ri)}")

    if errors:
        print("\nVALIDATION FAILED with errors:")
        for err in errors:
            print(f"  - {err}")
        return False
    else:
        print("\nALL VALIDATION CHECKS PASSED PERFECTLY!")
        print(f"  - Recipes: {len(df_recipes)}")
        print(f"  - Ingredients: {len(df_ingredients)}")
        print(f"  - Recipe-Ingredients: {len(df_ri)}")
        return True

if __name__ == "__main__":
    validate()
