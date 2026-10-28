import os
import re
import pandas as pd

RAW_DIR = os.environ.get(
    "CULINARY_DB_DIR",
    os.path.join(os.path.dirname(__file__), "..", "data", "dictionary")
)

class CulinaryKnowledge:
    def __init__(self, culinary_dir=RAW_DIR):
        self.culinary_dir = culinary_dir
        self.alias_map = {}            # variant_lower -> canonical_name
        self.ingredient_category = {}   # canonical_name_lower -> category
        self.compound_ingredients = set() # set of compound ingredient names lower
        self._load_knowledge()

    def _load_knowledge(self):
        # 1. Load 02_Ingredients.csv
        f_ing = os.path.join(self.culinary_dir, "02_Ingredients.csv")
        if os.path.exists(f_ing):
            df_ing = pd.read_csv(f_ing)
            for _, row in df_ing.iterrows():
                aliased = str(row['Aliased Ingredient Name']).strip()
                cat = str(row.get('Category', 'Other')).strip()
                if aliased:
                    self.alias_map[aliased.lower()] = aliased
                    self.ingredient_category[aliased.lower()] = self._normalize_category(cat)

                    # Also map synonyms if present
                    synonyms = str(row.get('Ingredient Synonyms', ''))
                    if synonyms and synonyms != 'nan':
                        for s in synonyms.split(','):
                            s_clean = s.strip().lower()
                            if s_clean:
                                self.alias_map[s_clean] = aliased

        # 2. Load 03_Compound_Ingredients.csv
        f_cmp = os.path.join(self.culinary_dir, "03_Compound_Ingredients.csv")
        if os.path.exists(f_cmp):
            df_cmp = pd.read_csv(f_cmp)
            for _, row in df_cmp.iterrows():
                cmp_name = str(row['Compound Ingredient Name']).strip()
                if cmp_name:
                    self.compound_ingredients.add(cmp_name.lower())
                    self.alias_map[cmp_name.lower()] = cmp_name
                    cat = str(row.get('Category', 'Spice')).strip()
                    self.ingredient_category[cmp_name.lower()] = self._normalize_category(cat)

        # 3. Load 04_Recipe-Ingredients_Aliases.csv (mappings)
        f_alias = os.path.join(self.culinary_dir, "04_Recipe-Ingredients_Aliases.csv")
        if os.path.exists(f_alias):
            df_alias = pd.read_csv(f_alias, usecols=['Original Ingredient Name', 'Aliased Ingredient Name'])
            df_alias = df_alias.drop_duplicates()
            for _, row in df_alias.iterrows():
                orig = str(row['Original Ingredient Name']).strip().lower()
                aliased = str(row['Aliased Ingredient Name']).strip()
                if orig and aliased and aliased != 'nan':
                    self.alias_map[orig] = aliased

        # 4. Add common Indian & Asian high-frequency culinary aliases not in standard datasets
        curated_aliases = {
            "dhania": ("Coriander", "Herb"),
            "dhaniya": ("Coriander", "Herb"),
            "kothmir": ("Coriander", "Herb"),
            "cilantro": ("Coriander", "Herb"),
            "fresh coriander": ("Coriander", "Herb"),
            "coriander leaves": ("Coriander", "Herb"),
            "coriander leaf": ("Coriander", "Herb"),
            "fresh cilantro": ("Coriander", "Herb"),
            "chopped coriander": ("Coriander", "Herb"),
            "spring onion": ("Green Onion", "Vegetable"),
            "spring onions": ("Green Onion", "Vegetable"),
            "scallion": ("Green Onion", "Vegetable"),
            "scallions": ("Green Onion", "Vegetable"),
            "green onion": ("Green Onion", "Vegetable"),
            "green onions": ("Green Onion", "Vegetable"),
            "hari mirch": ("Green Chilli", "Vegetable"),
            "green chilli": ("Green Chilli", "Vegetable"),
            "green chili": ("Green Chilli", "Vegetable"),
            "green chilies": ("Green Chilli", "Vegetable"),
            "green chillies": ("Green Chilli", "Vegetable"),
            "fresh green chillies": ("Green Chilli", "Vegetable"),
            "lal mirch powder": ("Red Chilli Powder", "Spice"),
            "red chilli powder": ("Red Chilli Powder", "Spice"),
            "red chili powder": ("Red Chilli Powder", "Spice"),
            "kashmiri red chilli powder": ("Kashmiri Red Chilli Powder", "Spice"),
            "haldi": ("Turmeric", "Spice"),
            "turmeric powder": ("Turmeric", "Spice"),
            "turmeric": ("Turmeric", "Spice"),
            "jeera": ("Cumin Seeds", "Spice"),
            "cumin seeds": ("Cumin Seeds", "Spice"),
            "cumin": ("Cumin Seeds", "Spice"),
            "jeera powder": ("Cumin Powder", "Spice"),
            "roasted cumin powder": ("Cumin Powder", "Spice"),
            "cumin powder": ("Cumin Powder", "Spice"),
            "adrak": ("Ginger", "Vegetable"),
            "ginger root": ("Ginger", "Vegetable"),
            "fresh ginger": ("Ginger", "Vegetable"),
            "grated ginger": ("Ginger", "Vegetable"),
            "ginger garlic paste": ("Ginger Garlic Paste", "Spice"),
            "adrak lehsun paste": ("Ginger Garlic Paste", "Spice"),
            "lehsun": ("Garlic", "Vegetable"),
            "lasun": ("Garlic", "Vegetable"),
            "garlic cloves": ("Garlic", "Vegetable"),
            "cloves of garlic": ("Garlic", "Vegetable"),
            "minced garlic": ("Garlic", "Vegetable"),
            "garlic": ("Garlic", "Vegetable"),
            "garam masala powder": ("Garam Masala", "Spice"),
            "garam masala": ("Garam Masala", "Spice"),
            "hing": ("Asafoetida", "Spice"),
            "asafoetida (hing)": ("Asafoetida", "Spice"),
            "asafoetida": ("Asafoetida", "Spice"),
            "sarson": ("Mustard Seeds", "Spice"),
            "mustard seeds (rai)": ("Mustard Seeds", "Spice"),
            "rai": ("Mustard Seeds", "Spice"),
            "mustard seeds": ("Mustard Seeds", "Spice"),
            "methi": ("Fenugreek", "Herb"),
            "kasuri methi": ("Kasuri Methi", "Herb"),
            "kasoori methi": ("Kasuri Methi", "Herb"),
            "dried fenugreek leaves": ("Kasuri Methi", "Herb"),
            "fenugreek leaves": ("Fenugreek", "Herb"),
            "fenugreek seeds": ("Fenugreek Seeds", "Spice"),
            "curry leaves": ("Curry Leaves", "Herb"),
            "kadi patta": ("Curry Leaves", "Herb"),
            "paneer": ("Paneer", "Dairy"),
            "cottage cheese (paneer)": ("Paneer", "Dairy"),
            "indian cottage cheese": ("Paneer", "Dairy"),
            "dahi": ("Yogurt", "Dairy"),
            "curd (dahi / yogurt)": ("Yogurt", "Dairy"),
            "curd": ("Yogurt", "Dairy"),
            "yogurt": ("Yogurt", "Dairy"),
            "plain yogurt": ("Yogurt", "Dairy"),
            "ghee": ("Ghee", "Dairy"),
            "clarified butter (ghee)": ("Ghee", "Dairy"),
            "clarified butter": ("Ghee", "Dairy"),
            "oil": ("Cooking Oil", "Oils"),
            "vegetable oil": ("Cooking Oil", "Oils"),
            "sunflower oil": ("Sunflower Oil", "Oils"),
            "mustard oil": ("Mustard Oil", "Oils"),
            "olive oil": ("Olive Oil", "Oils"),
            "extra virgin olive oil": ("Olive Oil", "Oils"),
            "sesame oil": ("Sesame Oil", "Oils"),
            "soy sauce": ("Soy Sauce", "Condiments"),
            "dark soy sauce": ("Soy Sauce", "Condiments"),
            "light soy sauce": ("Soy Sauce", "Condiments"),
            "oyster sauce": ("Oyster Sauce", "Condiments"),
            "fish sauce": ("Fish Sauce", "Condiments"),
            "rice vinegar": ("Rice Vinegar", "Condiments"),
            "mirin": ("Mirin", "Condiments"),
            "sesame seeds": ("Sesame Seeds", "Spice"),
            "tofu": ("Tofu", "Protein"),
            "firm tofu": ("Tofu", "Protein"),
            "boneless chicken": ("Chicken", "Meat"),
            "chicken breast": ("Chicken Breast", "Meat"),
            "chicken breasts": ("Chicken Breast", "Meat"),
            "chicken thighs": ("Chicken Thighs", "Meat"),
            "chicken thigh": ("Chicken Thighs", "Meat"),
            "chicken": ("Chicken", "Meat"),
            "mutton": ("Mutton", "Meat"),
            "lamb": ("Lamb", "Meat"),
            "prawns": ("Prawns", "Fish"),
            "shrimp": ("Prawns", "Fish"),
            "fish fillets": ("Fish", "Fish"),
            "fish fillet": ("Fish", "Fish"),
            "egg": ("Egg", "Dairy"),
            "eggs": ("Egg", "Dairy"),
            "whole eggs": ("Egg", "Dairy"),
            "salt": ("Salt", "Spice"),
            "salt to taste": ("Salt", "Spice"),
            "sugar": ("Sugar", "Other"),
            "water": ("Water", "Beverage"),
            "onion": ("Onion", "Vegetable"),
            "onions": ("Onion", "Vegetable"),
            "red onion": ("Onion", "Vegetable"),
            "tomato": ("Tomato", "Vegetable"),
            "tomatoes": ("Tomato", "Vegetable"),
            "potato": ("Potato", "Vegetable"),
            "potatoes": ("Potato", "Vegetable"),
            "capsicum": ("Bell Pepper", "Vegetable"),
            "green capsicum": ("Bell Pepper", "Vegetable"),
            "bell pepper": ("Bell Pepper", "Vegetable"),
            "carrots": ("Carrot", "Vegetable"),
            "carrot": ("Carrot", "Vegetable"),
            "peas": ("Green Peas", "Vegetable"),
            "green peas": ("Green Peas", "Vegetable"),
            "matar": ("Green Peas", "Vegetable"),
            "rice": ("Rice", "Grains"),
            "basmati rice": ("Basmati Rice", "Grains"),
            "jasmine rice": ("Jasmine Rice", "Grains"),
            "chana dal": ("Chana Dal", "Legume"),
            "toor dal": ("Toor Dal", "Legume"),
            "urad dal": ("Urad Dal", "Legume"),
            "moong dal": ("Moong Dal", "Legume"),
            "masoor dal": ("Masoor Dal", "Legume"),
            "atta": ("Wheat Flour", "Grains"),
            "whole wheat flour": ("Wheat Flour", "Grains"),
            "wheat flour": ("Wheat Flour", "Grains"),
            "maida": ("All Purpose Flour", "Grains"),
            "all purpose flour": ("All Purpose Flour", "Grains"),
            "besan": ("Gram Flour", "Grains"),
            "gram flour (besan)": ("Gram Flour", "Grains"),
            "gram flour": ("Gram Flour", "Grains"),
            "chickpea flour": ("Gram Flour", "Grains")
        }
        for k, (canon, cat) in curated_aliases.items():
            self.alias_map[k] = canon
            self.ingredient_category[canon.lower()] = cat

    def _normalize_category(self, cat):
        c = cat.strip().title()
        mapping = {
            "Plant": "Vegetables",
            "Vegetable": "Vegetables",
            "Vegetables": "Vegetables",
            "Fruit": "Fruits",
            "Fruits": "Fruits",
            "Spice": "Spices",
            "Spices": "Spices",
            "Herb": "Herbs",
            "Herbs": "Herbs",
            "Meat": "Meat",
            "Fish": "Seafood",
            "Seafood": "Seafood",
            "Dairy": "Dairy",
            "Cereal": "Grains",
            "Grains": "Grains",
            "Bakery": "Grains",
            "Legume": "Legumes",
            "Legumes": "Legumes",
            "Nuts & Seed": "Nuts & Seeds",
            "Nuts & Seeds": "Nuts & Seeds",
            "Essential Oil": "Oils",
            "Oils": "Oils",
            "Additive": "Condiments",
            "Condiments": "Condiments",
            "Beverage": "Beverages",
            "Beverages": "Beverages",
            "Dish": "Prepared"
        }
        return mapping.get(c, "Other")

    def canonicalize(self, raw_ingredient_name):
        if not raw_ingredient_name:
            return None, "Other"
        raw_clean = re.sub(r'[^a-zA-Z0-9\s\-]', ' ', str(raw_ingredient_name).lower())
        raw_clean = re.sub(r'\s+', ' ', raw_clean).strip()
        if not raw_clean:
            return None, "Other"

        # Direct alias lookup
        if raw_clean in self.alias_map:
            canon = self.alias_map[raw_clean]
            cat = self.ingredient_category.get(canon.lower(), "Other")
            return canon, cat

        # Try singularized variant
        singular = self._singularize(raw_clean)
        if singular in self.alias_map:
            canon = self.alias_map[singular]
            cat = self.ingredient_category.get(canon.lower(), "Other")
            return canon, cat

        # Fallback to Title Cased singular name
        canon = singular.title()
        cat = self._infer_category(canon)
        return canon, cat

    def _singularize(self, text):
        words = text.split()
        res = []
        for w in words:
            if len(w) > 3:
                if w.endswith("ies") and len(w) > 4:
                    w = w[:-3] + "y"
                elif w.endswith("es") and (w.endswith("shes") or w.endswith("ches") or w.endswith("toes")):
                    w = w[:-2]
                elif w.endswith("s") and not w.endswith(("ss", "us", "is")):
                    w = w[:-1]
            res.append(w)
        return " ".join(res)

    def _infer_category(self, name):
        nl = name.lower()
        if any(x in nl for x in ["chilli", "pepper", "powder", "masala", "clove", "cinnamon", "cardamom", "cumin", "turmeric", "seed", "mustard"]):
            return "Spices"
        if any(x in nl for x in ["leaf", "leaves", "coriander", "mint", "basil", "parsley", "cilantro", "methi"]):
            return "Herbs"
        if any(x in nl for x in ["onion", "garlic", "tomato", "potato", "carrot", "bean", "gourd", "spinach", "cabbage", "cauliflower", "pea"]):
            return "Vegetables"
        if any(x in nl for x in ["chicken", "mutton", "lamb", "pork", "beef", "meat"]):
            return "Meat"
        if any(x in nl for x in ["fish", "prawn", "shrimp", "salmon", "tuna", "crab"]):
            return "Seafood"
        if any(x in nl for x in ["milk", "curd", "yogurt", "cheese", "paneer", "butter", "ghee", "cream"]):
            return "Dairy"
        if any(x in nl for x in ["rice", "flour", "wheat", "noodle", "bread", "oat", "quinoa"]):
            return "Grains"
        if any(x in nl for x in ["dal", "lentil", "chana", "chickpea", "rajma"]):
            return "Legumes"
        if any(x in nl for x in ["oil", "ghee"]):
            return "Oils"
        if any(x in nl for x in ["sauce", "vinegar", "paste", "syrup", "ketchup"]):
            return "Condiments"
        return "Other"

if __name__ == "__main__":
    ck = CulinaryKnowledge()
    print(f"Loaded {len(ck.alias_map)} aliases, {len(ck.compound_ingredients)} compound ingredients.")
    tests = [
        "scallions", "fresh cilantro", "hari mirch", "garlic cloves",
        "dahi", "2 cups basmati rice", "boneless chicken thighs", "garam masala"
    ]
    for t in tests:
        c, cat = ck.canonicalize(t)
        print(f"  '{t}' -> '{c}' [{cat}]")
