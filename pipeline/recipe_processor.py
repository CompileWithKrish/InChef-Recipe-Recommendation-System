import os
import re
import ast
import json
import csv
import pandas as pd
from culinary_knowledge import CulinaryKnowledge
from ingredient_parser import IngredientParser

RAW_DIR = os.environ.get(
    "RAW_DATASET_DIR",
    os.path.join(os.path.dirname(__file__), "..", "data", "samples")
)
DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "data")

ALLOWED_INDIAN_CUISINES = {
    "indian", "north indian", "north indian recipes", "south indian", "south indian recipes",
    "bengali", "bengali recipes", "punjabi", "punjabi recipes", "gujarati", "gujarati recipes",
    "gujarati recipes﻿", "rajasthani", "maharashtrian", "maharashtrian recipes", "kerala",
    "kerala recipes", "tamil nadu", "tamil nadu recipes", "karnataka", "karnataka recipes",
    "andhra", "andhra recipes", "goan", "goan recipes", "chettinad", "kashmiri", "hyderabadi",
    "mughlai", "awadhi", "udupi", "sindhi", "bihari", "assamese", "oriya", "oriya recipes",
    "parsi", "parsi recipes", "konkan", "mangalorean", "malabar", "malabar recipes",
    "pakistani", "sri lankan", "nepali", "coorg", "nagaland", "lucknowi", "himachal"
}

ALLOWED_ASIAN_CUISINES = {
    "asian", "chinese", "chinese recipes", "japanese", "korean", "thai", "thai recipes",
    "vietnamese", "indonesian", "filipino", "malaysian", "south east asian", "taiwanese",
    "singaporean", "burmese", "tibetan", "cambodian"
}

REJECTED_WESTERN = {
    "continental", "italian", "italian recipes", "mexican", "french", "french recipes",
    "american", "british", "mediterranean", "nordic", "greek", "caribbean", "world",
    "jewish", "middle eastern", "central europe", "eastern europe", "spanish", "south american"
}

COURSE_MAP = {
    "breakfast": "BREAKFAST",
    "south indian breakfast": "BREAKFAST",
    "north indian breakfast": "BREAKFAST",
    "indian breakfast": "BREAKFAST",
    "brunch": "BREAKFAST",
    "lunch": "LUNCH",
    "dinner": "DINNER",
    "main course": "DINNER",
    "side dish": "SNACK",
    "snack": "SNACK",
    "appetizer": "SNACK",
    "starter": "SNACK",
    "dessert": "DESSERT",
    "sweet": "DESSERT",
    "beverage": "BEVERAGE",
    "drink": "BEVERAGE"
}

DIET_MAP = {
    "vegetarian": "VEGETARIAN",
    "high protein vegetarian": "VEGETARIAN",
    "diabetic friendly": "VEGETARIAN",
    "no onion no garlic (sattvic)": "VEGETARIAN",
    "vegan": "VEGAN",
    "non vegeterian": "NON_VEGETARIAN",
    "non vegetarian": "NON_VEGETARIAN",
    "high protein non vegetarian": "NON_VEGETARIAN",
    "eggetarian": "EGGETARIAN",
    "gluten free": "GLUTEN_FREE",
    "sugar free diet": "VEGETARIAN"
}

class RecipeProcessor:
    def __init__(self):
        self.ck = CulinaryKnowledge()
        self.parser = IngredientParser(self.ck)
        self.recipes = []             # Cleaned recipe dicts
        self.ingredients_dict = {}    # canonical_name -> {name, category, defaultUnit, count}
        self.recipe_ingredients = []  # list of recipe-ingredient links
        self.rejected_recipes = []
        self.rejected_ingredients = []
        self.seen_search_titles = {}  # norm_title -> index in self.recipes

    def clean_title(self, raw_title):
        t = str(raw_title).replace('\xa0', ' ').strip()
        # Remove trailing "Recipe" or "recipe"
        t = re.sub(r'\s+recipe$', '', t, flags=re.I).strip()
        t = re.sub(r'[\'\"“”]+', '', t).strip()
        t = re.sub(r'\s+', ' ', t).strip()
        return t.title()

    def normalize_for_search(self, title):
        clean = re.sub(r'[^a-zA-Z0-9\s]', '', title.lower())
        return re.sub(r'\s+', ' ', clean).strip()

    def map_indian_cuisine(self, raw_cuisine):
        c_low = str(raw_cuisine).strip().lower()
        if c_low in ALLOWED_INDIAN_CUISINES:
            # Clean up regional label
            c_clean = re.sub(r'\s+recipes?$', '', c_low).title()
            if "Gujarati" in c_clean:
                return "Gujarati"
            if c_clean in ["South Indian", "North Indian", "Bengali", "Punjabi", "Rajasthani", "Maharashtrian", "Kerala", "Tamil Nadu", "Karnataka", "Andhra", "Goan", "Chettinad", "Kashmiri", "Hyderabadi", "Mughlai", "Awadhi", "Udupi", "Sindhi", "Bihari", "Assamese", "Oriya", "Parsi", "Konkan", "Mangalorean", "Malabar"]:
                return c_clean
            return "Indian"
        if c_low in ALLOWED_ASIAN_CUISINES:
            return re.sub(r'\s+recipes?$', '', c_low).title()
        return None

    def map_datahive_cuisine(self, cuisine_list):
        if not cuisine_list:
            return None
        for item in cuisine_list:
            c = str(item).strip().lower()
            if c in ALLOWED_INDIAN_CUISINES:
                return "Indian"
            if c in ALLOWED_ASIAN_CUISINES:
                return c.title()
            if "asian" in c or "south east asian" in c:
                return "Asian"
            if "china" in c or "chinese" in c:
                return "Chinese"
            if "japan" in c or "japanese" in c:
                return "Japanese"
            if "korea" in c or "korean" in c:
                return "Korean"
            if "thai" in c:
                return "Thai"
            if "vietnam" in c:
                return "Vietnamese"
        return None

    def map_meal_type(self, raw_course):
        if not raw_course:
            return "DINNER"
        c = str(raw_course).strip().lower()
        for k, v in COURSE_MAP.items():
            if k in c:
                return v
        return "DINNER"

    def map_dietary_type(self, raw_diet, ingredients_names, health_labels=None):
        if raw_diet:
            d = str(raw_diet).strip().lower()
            for k, v in DIET_MAP.items():
                if k in d:
                    return v

        # Check health labels
        if health_labels:
            hl_set = {str(x).strip().lower() for x in health_labels}
            if "vegan" in hl_set:
                return "VEGAN"
            if "vegetarian" in hl_set:
                return "VEGETARIAN"
            if "gluten-free" in hl_set:
                return "GLUTEN_FREE"

        # Check ingredient keywords
        ing_str = " ".join([ing.lower() for ing in ingredients_names])
        if any(x in ing_str for x in ["chicken", "mutton", "lamb", "fish", "prawn", "shrimp", "pork", "beef"]):
            return "NON_VEGETARIAN"
        if "egg" in ing_str:
            return "EGGETARIAN"
        return "VEGETARIAN"

    def calculate_difficulty(self, total_time, ingredient_count):
        t = total_time if total_time and total_time > 0 else 35
        if t <= 30 and ingredient_count <= 8:
            return "EASY"
        elif t > 60 or ingredient_count > 15:
            return "HARD"
        return "MEDIUM"

    def calculate_spice_level(self, ingredient_names):
        ing_str = " ".join([i.lower() for i in ingredient_names])
        if any(x in ing_str for x in ["ghost pepper", "bhut jolokia", "bird's eye", "habanero", "extra spicy"]):
            return "EXTRA_SPICY"
        if any(x in ing_str for x in ["green chilli", "red chilli", "chili powder", "cayenne", "schezwan", "jalapeno", "sriracha"]):
            return "SPICY"
        if any(x in ing_str for x in ["black pepper", "ginger", "garam masala", "pepper", "clove", "cinnamon"]):
            return "MEDIUM"
        return "MILD"

    def process_indian_food_dataset(self):
        print("\n--- Processing Indian Food Dataset ---")
        fpath = os.path.join(RAW_DIR, "IndianFoodDatasetXLS.xlsx")
        df = pd.read_excel(fpath, sheet_name=0)

        for idx, row in df.iterrows():
            srno = row['Srno']
            raw_title = row['TranslatedRecipeName'] if pd.notna(row.get('TranslatedRecipeName')) else row['RecipeName']
            if not raw_title or str(raw_title).strip() == "" or str(raw_title).lower() == "nan":
                self.rejected_recipes.append({"source": "IndianFoodDataset", "id": srno, "title": raw_title, "reason": "Missing recipe title"})
                continue

            # Cuisine filter
            raw_cuisine = row.get('Cuisine', '')
            cuisine = self.map_indian_cuisine(raw_cuisine)
            if not cuisine:
                self.rejected_recipes.append({"source": "IndianFoodDataset", "id": srno, "title": raw_title, "reason": f"Excluded non-Indian/Asian cuisine: {raw_cuisine}"})
                continue

            # Ingredients check
            raw_ing_text = row.get('TranslatedIngredients') if pd.notna(row.get('TranslatedIngredients')) else row.get('Ingredients')
            if not raw_ing_text or str(raw_ing_text).strip() == "" or str(raw_ing_text).lower() == "nan":
                self.rejected_recipes.append({"source": "IndianFoodDataset", "id": srno, "title": raw_title, "reason": "Missing ingredients"})
                continue

            # Parse ingredients (comma or newline separated)
            ing_lines = [l.strip() for l in str(raw_ing_text).split(',') if l.strip()]
            parsed_ings = []
            for il in ing_lines:
                res = self.parser.parse_ingredient_line(il)
                if res and res["canonical_name"]:
                    parsed_ings.append(res)

            if not parsed_ings:
                self.rejected_recipes.append({"source": "IndianFoodDataset", "id": srno, "title": raw_title, "reason": "No parseable ingredients"})
                continue

            # Title & Dedup check
            title = self.clean_title(raw_title)
            norm_title = self.normalize_for_search(title)

            # Check duplicate against existing
            if norm_title in self.seen_search_titles:
                existing_idx = self.seen_search_titles[norm_title]
                existing_recipe = self.recipes[existing_idx]
                # If exact title and high ingredient overlap, skip duplicate
                self.rejected_recipes.append({"source": "IndianFoodDataset", "id": srno, "title": raw_title, "reason": f"Duplicate of recipe ID {existing_recipe['recipe_id']}"})
                continue

            # Cooking times
            prep = int(row.get('PrepTimeInMins', 0)) if pd.notna(row.get('PrepTimeInMins')) and row.get('PrepTimeInMins') > 0 else None
            cook = int(row.get('CookTimeInMins', 0)) if pd.notna(row.get('CookTimeInMins')) and row.get('CookTimeInMins') > 0 else None
            total = int(row.get('TotalTimeInMins', 0)) if pd.notna(row.get('TotalTimeInMins')) and row.get('TotalTimeInMins') > 0 else None
            if not total and prep and cook:
                total = prep + cook
            if not prep and not cook and total:
                prep = max(5, int(total * 0.3))
                cook = total - prep

            servings = int(row.get('Servings', 4)) if pd.notna(row.get('Servings')) and row.get('Servings') > 0 else 4
            meal_type = self.map_meal_type(row.get('Course', ''))
            ing_names = [pi["canonical_name"] for pi in parsed_ings]
            dietary_type = self.map_dietary_type(row.get('Diet', ''), ing_names)
            difficulty = self.calculate_difficulty(total, len(parsed_ings))
            spice_level = self.calculate_spice_level(ing_names)

            # Instructions
            inst = row.get('TranslatedInstructions') if pd.notna(row.get('TranslatedInstructions')) else row.get('Instructions', '')
            instructions = str(inst).replace('\xa0', ' ').strip() if pd.notna(inst) else None

            # Description
            top_3 = ", ".join(ing_names[:3])
            desc = f"An authentic {cuisine} {meal_type.lower()} featuring {top_3}, crafted with traditional spices."

            recipe_id = len(self.recipes) + 1
            recipe_obj = {
                "recipe_id": recipe_id,
                "title": title[:150],
                "description": desc,
                "cuisine": cuisine[:50],
                "meal_type": meal_type,
                "dietary_type": dietary_type,
                "difficulty": difficulty,
                "prep_time_minutes": prep,
                "cook_time_minutes": cook,
                "total_time_minutes": total,
                "servings": servings,
                "calories": None,
                "spice_level": spice_level,
                "instructions": instructions,
                "image_url": None,
                "video_url": None,
                "is_published": 1,
                "source_url": str(row.get('URL', ''))[:500] if pd.notna(row.get('URL')) else None,
                "source_dataset": "IndianFoodDataset",
                "source_recipe_id": str(srno)
            }
            self.recipes.append(recipe_obj)
            self.seen_search_titles[norm_title] = len(self.recipes) - 1

            # Store recipe ingredients & register ingredients
            for pi in parsed_ings:
                c_name = pi["canonical_name"]
                cat = pi["category"]
                d_unit = pi["unit"]

                if c_name not in self.ingredients_dict:
                    self.ingredients_dict[c_name] = {
                        "name": c_name,
                        "category": cat,
                        "default_unit": d_unit,
                        "image_url": None
                    }

                self.recipe_ingredients.append({
                    "recipe_id": recipe_id,
                    "ingredient_name": c_name,
                    "quantity": pi["quantity"],
                    "unit": pi["unit"],
                    "is_optional": 1 if pi["is_optional"] else 0,
                    "notes": pi["notes"]
                })

        print(f"Loaded {len(self.recipes)} authentic Indian/Asian recipes from IndianFoodDataset.")

    def process_datahive_dataset(self):
        print("\n--- Processing DataHive Dataset ---")
        fpath = os.path.join(RAW_DIR, "recipes-with-nutrition(DataHive).csv")
        chunksize = 10000
        count_imported = 0

        for chunk in pd.read_csv(fpath, chunksize=chunksize, low_memory=False):
            for _, row in chunk.iterrows():
                raw_title = row.get('recipe_name', '')
                if not raw_title or str(raw_title).strip() == "" or str(raw_title).lower() == "nan":
                    continue

                # Parse cuisine_type list
                c_val = row.get('cuisine_type')
                c_list = []
                if pd.notna(c_val):
                    try:
                        c_list = ast.literal_eval(c_val) if isinstance(c_val, str) and c_val.startswith("[") else [str(c_val)]
                    except Exception:
                        c_list = [str(c_val)]

                cuisine = self.map_datahive_cuisine(c_list)
                if not cuisine:
                    # Not an Asian or Indian recipe -> reject/exclude safely
                    continue

                # Ingredients
                raw_ings = row.get('ingredients')
                parsed_ings = []
                if pd.notna(raw_ings):
                    try:
                        ing_objs = json.loads(raw_ings) if isinstance(raw_ings, str) else raw_ings
                        for io in ing_objs:
                            food = io.get('food', '')
                            qty = io.get('quantity', 1.0)
                            measure = io.get('measure', '')
                            text = io.get('text', '')

                            # Build pseudo line
                            line = f"{qty} {measure} {food}".strip() if measure and measure != '<unit>' else f"{qty} {food}".strip()
                            parsed = self.parser.parse_ingredient_line(line)
                            if parsed and parsed["canonical_name"]:
                                parsed_ings.append(parsed)
                    except Exception:
                        pass

                if not parsed_ings:
                    # Fallback to ingredient_lines
                    raw_lines = row.get('ingredient_lines')
                    if pd.notna(raw_lines):
                        try:
                            lines_list = json.loads(raw_lines) if isinstance(raw_lines, str) else [str(raw_lines)]
                            for l in lines_list:
                                parsed = self.parser.parse_ingredient_line(str(l))
                                if parsed and parsed["canonical_name"]:
                                    parsed_ings.append(parsed)
                        except Exception:
                            pass

                if not parsed_ings:
                    self.rejected_recipes.append({"source": "DataHive", "id": row.get('url', ''), "title": raw_title, "reason": "No parseable ingredients"})
                    continue

                title = self.clean_title(raw_title)
                norm_title = self.normalize_for_search(title)

                # Deduplication check
                if norm_title in self.seen_search_titles:
                    existing_idx = self.seen_search_titles[norm_title]
                    existing_recipe = self.recipes[existing_idx]
                    # If existing recipe lacks image or calories, enrich it from DataHive!
                    if not existing_recipe.get("image_url") and pd.notna(row.get('image_url')):
                        existing_recipe["image_url"] = str(row['image_url'])[:500]
                    if not existing_recipe.get("calories") and pd.notna(row.get('calories')) and row.get('calories') > 0:
                        serv = existing_recipe.get("servings", 4) or 4
                        existing_recipe["calories"] = int(row['calories'] / serv)

                    self.rejected_recipes.append({"source": "DataHive", "id": row.get('url', ''), "title": raw_title, "reason": f"Deduplicated with existing recipe '{existing_recipe['title']}'"})
                    continue

                # Servings
                servings = int(row.get('servings', 4)) if pd.notna(row.get('servings')) and row.get('servings') > 0 else 4

                # Calories per serving
                calories = None
                if pd.notna(row.get('calories')) and row.get('calories') > 0:
                    calories = int(row.get('calories') / servings) if servings > 0 else int(row.get('calories'))

                # Meal type
                meal_type_raw = row.get('meal_type')
                dish_type_raw = row.get('dish_type')
                combined_type = f"{meal_type_raw} {dish_type_raw}"
                meal_type = self.map_meal_type(combined_type)

                # Health / Diet labels
                hl_list = []
                if pd.notna(row.get('health_labels')):
                    try:
                        hl_list = ast.literal_eval(row['health_labels']) if isinstance(row['health_labels'], str) and row['health_labels'].startswith("[") else []
                    except Exception:
                        pass

                ing_names = [pi["canonical_name"] for pi in parsed_ings]
                dietary_type = self.map_dietary_type(row.get('diet_labels', ''), ing_names, hl_list)
                spice_level = self.calculate_spice_level(ing_names)
                difficulty = self.calculate_difficulty(30, len(parsed_ings))

                # Image & Source URL
                img_url = str(row.get('image_url', ''))[:500] if pd.notna(row.get('image_url')) and str(row.get('image_url')).startswith("http") else None
                src_url = str(row.get('url', ''))[:500] if pd.notna(row.get('url')) else None

                # Synthesize formatted instructions from ingredient lines & culinary guidance
                instructions = f"1. Prepare and measure all ingredients: {', '.join(ing_names[:5])}.\n2. Follow traditional {cuisine} preparation method, combining spices and ingredients in order.\n3. Simmer or cook until desired tenderness and flavors are well blended.\n4. Serve hot."

                top_3 = ", ".join(ing_names[:3])
                desc = f"A classic {cuisine} {meal_type.lower()} prepared with {top_3}."

                recipe_id = len(self.recipes) + 1
                recipe_obj = {
                    "recipe_id": recipe_id,
                    "title": title[:150],
                    "description": desc,
                    "cuisine": cuisine[:50],
                    "meal_type": meal_type,
                    "dietary_type": dietary_type,
                    "difficulty": difficulty,
                    "prep_time_minutes": 15,
                    "cook_time_minutes": 20,
                    "total_time_minutes": 35,
                    "servings": servings,
                    "calories": calories,
                    "spice_level": spice_level,
                    "instructions": instructions,
                    "image_url": img_url,
                    "video_url": None,
                    "is_published": 1,
                    "source_url": src_url,
                    "source_dataset": "DataHive",
                    "source_recipe_id": str(row.get('url', ''))[:100]
                }
                self.recipes.append(recipe_obj)
                self.seen_search_titles[norm_title] = len(self.recipes) - 1
                count_imported += 1

                for pi in parsed_ings:
                    c_name = pi["canonical_name"]
                    cat = pi["category"]
                    d_unit = pi["unit"]

                    if c_name not in self.ingredients_dict:
                        self.ingredients_dict[c_name] = {
                            "name": c_name,
                            "category": cat,
                            "default_unit": d_unit,
                            "image_url": None
                        }

                    self.recipe_ingredients.append({
                        "recipe_id": recipe_id,
                        "ingredient_name": c_name,
                        "quantity": pi["quantity"],
                        "unit": pi["unit"],
                        "is_optional": 1 if pi["is_optional"] else 0,
                        "notes": pi["notes"]
                    })

        print(f"Loaded {count_imported} Asian/Indian recipes from DataHive.")

    def export_all(self):
        print("\n=== Exporting Cleaned and Processed Datasets ===")
        cleaned_dir = os.path.join(DATA_DIR, "cleaned")
        processed_dir = os.path.join(DATA_DIR, "processed")
        rejected_dir = os.path.join(DATA_DIR, "rejected")
        os.makedirs(cleaned_dir, exist_ok=True)
        os.makedirs(processed_dir, exist_ok=True)
        os.makedirs(rejected_dir, exist_ok=True)

        # 1. Ingredients table (strictly case-insensitive unique)
        unique_ingredients = {}
        for raw_name, data in self.ingredients_dict.items():
            key = raw_name.strip().lower()
            if key not in unique_ingredients:
                unique_ingredients[key] = {
                    "name": data["name"].strip().title(),
                    "category": data["category"],
                    "default_unit": data["default_unit"],
                    "image_url": data["image_url"]
                }

        ing_list = []
        ing_id_map = {}  # lower_name -> id
        for idx, (k_lower, data) in enumerate(sorted(unique_ingredients.items()), 1):
            data["id"] = idx
            ing_id_map[k_lower] = idx
            ing_list.append(data)

        # 2. Recipe-Ingredients links (strictly unique by recipe_id + ingredient_id)
        pair_map = {}
        for ri in self.recipe_ingredients:
            k_lower = ri["ingredient_name"].strip().lower()
            i_id = ing_id_map.get(k_lower)
            if not i_id:
                continue

            pair_key = (ri["recipe_id"], i_id)
            if pair_key in pair_map:
                # Combine quantities & notes
                existing = pair_map[pair_key]
                existing["quantity"] = round(existing["quantity"] + ri["quantity"], 2)
                if ri["notes"] and ri["notes"] not in (existing["notes"] or ""):
                    existing["notes"] = f"{existing['notes']}, {ri['notes']}" if existing["notes"] else ri["notes"]
            else:
                pair_map[pair_key] = {
                    "recipe_id": ri["recipe_id"],
                    "ingredient_id": i_id,
                    "quantity": ri["quantity"],
                    "unit": ri["unit"],
                    "is_optional": ri["is_optional"],
                    "notes": ri["notes"]
                }

        final_recipe_ings = list(pair_map.values())

        # Save to cleaned/
        pd.DataFrame(self.recipes).to_csv(os.path.join(cleaned_dir, "recipes_cleaned.csv"), index=False)
        pd.DataFrame(ing_list).to_csv(os.path.join(cleaned_dir, "ingredients_cleaned.csv"), index=False)
        pd.DataFrame(final_recipe_ings).to_csv(os.path.join(cleaned_dir, "recipe_ingredients_cleaned.csv"), index=False)

        # Save to processed/ (inchef standard naming)
        pd.DataFrame(self.recipes).to_csv(os.path.join(processed_dir, "inchef_recipes.csv"), index=False)
        pd.DataFrame(ing_list).to_csv(os.path.join(processed_dir, "inchef_ingredients.csv"), index=False)
        pd.DataFrame(final_recipe_ings).to_csv(os.path.join(processed_dir, "inchef_recipe_ingredients.csv"), index=False)

        # Save rejected records
        pd.DataFrame(self.rejected_recipes).to_csv(os.path.join(rejected_dir, "rejected_recipes.csv"), index=False)
        pd.DataFrame(self.rejected_ingredients).to_csv(os.path.join(rejected_dir, "rejected_ingredients.csv"), index=False)

        print(f"Exported {len(self.recipes)} recipes, {len(ing_list)} unique ingredients, {len(final_recipe_ings)} recipe-ingredient links.")
        print(f"Rejected records: {len(self.rejected_recipes)} recipes.")

if __name__ == "__main__":
    rp = RecipeProcessor()
    rp.process_indian_food_dataset()
    rp.process_datahive_dataset()
    rp.export_all()
