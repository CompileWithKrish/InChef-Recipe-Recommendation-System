import re
import unicodedata
from culinary_knowledge import CulinaryKnowledge

FRACTION_MAP = {
    '½': 0.5, '⅓': 0.333, '⅔': 0.667, '¼': 0.25, '¾': 0.75,
    '⅕': 0.2, '⅖': 0.4, '⅗': 0.6, '⅘': 0.8, '⅙': 0.167, '⅚': 0.833,
    '⅛': 0.125, '⅜': 0.375, '⅝': 0.625, '⅞': 0.875
}

UNIT_MAP = {
    # GRAMS
    'g': 'GRAMS', 'gm': 'GRAMS', 'gms': 'GRAMS', 'gram': 'GRAMS', 'grams': 'GRAMS',
    # KILOGRAMS
    'kg': 'KILOGRAMS', 'kgs': 'KILOGRAMS', 'kilo': 'KILOGRAMS', 'kilogram': 'KILOGRAMS', 'kilograms': 'KILOGRAMS',
    # MILLILITERS
    'ml': 'MILLILITERS', 'mls': 'MILLILITERS', 'milliliter': 'MILLILITERS', 'milliliters': 'MILLILITERS',
    # LITERS
    'l': 'LITERS', 'ltr': 'LITERS', 'liter': 'LITERS', 'liters': 'LITERS',
    # CUPS
    'cup': 'CUPS', 'cups': 'CUPS',
    # TABLESPOONS
    'tbsp': 'TABLESPOONS', 'tbsps': 'TABLESPOONS', 'tablespoon': 'TABLESPOONS', 'tablespoons': 'TABLESPOONS', 'tbs': 'TABLESPOONS',
    # TEASPOONS
    'tsp': 'TEASPOONS', 'tsps': 'TEASPOONS', 'teaspoon': 'TEASPOONS', 'teaspoons': 'TEASPOONS',
    # PINCH
    'pinch': 'PINCH', 'pinches': 'PINCH',
    # PIECES
    'piece': 'PIECES', 'pieces': 'PIECES', 'pc': 'PIECES', 'pcs': 'PIECES',
    'clove': 'PIECES', 'cloves': 'PIECES', 'slice': 'PIECES', 'slices': 'PIECES',
    'sprig': 'PIECES', 'sprigs': 'PIECES', 'leaf': 'PIECES', 'leaves': 'PIECES',
    'stalk': 'PIECES', 'stalks': 'PIECES', 'bunch': 'PIECES', 'bunches': 'PIECES',
    'head': 'PIECES', 'heads': 'PIECES', 'can': 'PIECES', 'cans': 'PIECES',
    'inch': 'PIECES', 'inches': 'PIECES'
}

PREP_WORDS = [
    'chopped', 'finely chopped', 'roughly chopped', 'minced', 'sliced', 'thinly sliced',
    'diced', 'peeled', 'grated', 'shredded', 'crushed', 'ground', 'powdered', 'roasted',
    'to taste', 'deseeded', 'slit', 'cut into pieces', 'boiled', 'cooked', 'soaked',
    'washed', 'drained', 'sifted', 'whisked', 'beaten', 'melted', 'warm', 'cold',
    'fresh', 'dry', 'dried', 'optional', 'for garnishing', 'as required', 'as needed'
]

class IngredientParser:
    def __init__(self, culinary_knowledge: CulinaryKnowledge):
        self.ck = culinary_knowledge

    def parse_quantity(self, text):
        """Extract numeric quantity from prefix of text, handling fractions and unicode."""
        text = text.strip()
        # Replace unicode fractions
        for frac_char, val in FRACTION_MAP.items():
            if frac_char in text:
                text = text.replace(frac_char, f" {val} ")

        # Match range like "1-2" -> 1.5, "1 to 2" -> 1.5
        range_match = re.match(r'^(\d+(?:\.\d+)?)\s*(?:-|to)\s*(\d+(?:\.\d+)?)\b', text, re.I)
        if range_match:
            q1 = float(range_match.group(1))
            q2 = float(range_match.group(2))
            rest = text[range_match.end():].strip()
            return round((q1 + q2) / 2.0, 2), rest

        # Match compound fraction like "1 1/2"
        comp_match = re.match(r'^(\d+)\s+(\d+)\s*/\s*(\d+)\b', text)
        if comp_match:
            whole = float(comp_match.group(1))
            num = float(comp_match.group(2))
            den = float(comp_match.group(3))
            rest = text[comp_match.end():].strip()
            return round(whole + (num / den), 2), rest

        # Match simple fraction like "1/2"
        frac_match = re.match(r'^(\d+)\s*/\s*(\d+)\b', text)
        if frac_match:
            num = float(frac_match.group(1))
            den = float(frac_match.group(2))
            rest = text[frac_match.end():].strip()
            return round(num / den, 2), rest

        # Match single number like "2" or "1.5"
        num_match = re.match(r'^(\d+(?:\.\d+)?)\b', text)
        if num_match:
            val = float(num_match.group(1))
            rest = text[num_match.end():].strip()
            return round(val, 2), rest

        # No quantity found
        return 1.0, text

    def parse_ingredient_line(self, line):
        """
        Parses a raw ingredient string into:
        - canonical_name: str
        - category: str
        - quantity: float
        - unit: str (valid MeasurementUnit enum)
        - notes: str
        - is_optional: bool
        """
        if not line or not isinstance(line, str):
            return None

        orig_line = line.strip()
        if not orig_line:
            return None

        # Check optional
        is_optional = bool(re.search(r'\boptional\b', orig_line, re.I))

        # Check "salt to taste"
        if re.search(r'\bsalt\b', orig_line, re.I) and re.search(r'to taste', orig_line, re.I):
            return {
                "canonical_name": "Salt",
                "category": "Spices",
                "quantity": 1.0,
                "unit": "PINCH",
                "notes": "to taste",
                "is_optional": False
            }

        # Handle split notes with hyphen or comma or parentheses
        # e.g. "6 Karela (Bitter Gourd/ Pavakkai) - deseeded"
        notes = []
        clean_text = orig_line

        # Extract parenthetical notes (e.g. "(Bitter Gourd/ Pavakkai)")
        parens = re.findall(r'\((.*?)\)', clean_text)
        for p in parens:
            notes.append(p.strip())
        clean_text = re.sub(r'\(.*?\)', ' ', clean_text)

        # Check hyphen notes
        if '-' in clean_text:
            parts = clean_text.split('-', 1)
            clean_text = parts[0].strip()
            notes.append(parts[1].strip())

        # Extract quantity
        qty, clean_text = self.parse_quantity(clean_text)

        # Extract unit
        words = clean_text.split()
        unit = "PIECES"
        if words:
            first_word = words[0].lower().strip(".,")
            if first_word in UNIT_MAP:
                unit = UNIT_MAP[first_word]
                words = words[1:]
            elif len(words) > 1 and f"{first_word} {words[1].lower().strip('.,')}" in UNIT_MAP:
                unit = UNIT_MAP[f"{first_word} {words[1].lower().strip('.,')}"]
                words = words[2:]

        clean_ing_text = " ".join(words).strip()

        # Check for trailing comma notes: "1 cup milk, warm"
        if ',' in clean_ing_text:
            c_parts = clean_ing_text.split(',')
            clean_ing_text = c_parts[0].strip()
            notes.extend([p.strip() for p in c_parts[1:] if p.strip()])

        # Clean prep words from ingredient name if found at end
        for pw in PREP_WORDS:
            pattern = re.compile(rf'\b{pw}\b', re.I)
            if pattern.search(clean_ing_text):
                notes.append(pw)
                clean_ing_text = pattern.sub('', clean_ing_text).strip()

        clean_ing_text = re.sub(r'\s+', ' ', clean_ing_text).strip()
        if not clean_ing_text:
            clean_ing_text = orig_line

        # Canonicalize using CulinaryKnowledge
        canon_name, category = self.ck.canonicalize(clean_ing_text)
        if not canon_name:
            canon_name = clean_ing_text.title()

        note_str = ", ".join([n for n in notes if n and len(n) > 1])[:250]

        return {
            "canonical_name": canon_name[:100],
            "category": category[:50],
            "quantity": qty if qty > 0 else 1.0,
            "unit": unit,
            "notes": note_str if note_str else None,
            "is_optional": is_optional
        }

if __name__ == "__main__":
    ck = CulinaryKnowledge()
    parser = IngredientParser(ck)
    test_lines = [
        "6 Karela (Bitter Gourd/ Pavakkai) - deseeded",
        "Salt - to taste",
        "1 Onion - thinly sliced",
        "3 tablespoon Gram flour (besan)",
        "1/2 teaspoon Turmeric powder (Haldi)",
        "2 cups basmati rice, washed and soaked",
        "500 grams boneless chicken breasts, cut into cubes",
        "1 tbsp soy sauce",
        "3 cloves garlic, minced",
        "1 pinch asafoetida (hing)"
    ]
    for line in test_lines:
        res = parser.parse_ingredient_line(line)
        print(f"'{line}'\n  -> {res}")
