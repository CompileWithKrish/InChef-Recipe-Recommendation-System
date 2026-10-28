import os
import sys
import pymysql

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PASS = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "inchef_db")

if not DB_PASS:
    print("Error: DB_PASSWORD environment variable is not set.", file=sys.stderr)
    print("Please set DB_PASSWORD in your environment or .env file before running this script.", file=sys.stderr)
    sys.exit(1)

conn = pymysql.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASS,
    database=DB_NAME,
    charset="utf8mb4"
)
cur = conn.cursor()

# Check favorites
cur.execute("""
    SELECT f.id, f.user_id, f.recipe_id, r.title, r.source_dataset, r.source_recipe_id
    FROM favorites f
    JOIN recipes r ON f.recipe_id = r.id
""")
favs = cur.fetchall()
print("Current Favorites:")
for f in favs:
    print(" ", f)

# Check user pantry
cur.execute("""
    SELECT count(distinct ui.ingredient_id) FROM user_ingredients ui
""")
print("\nDistinct ingredients in user pantry:", cur.fetchone()[0])

# Check ingredients used by IndianFoodDataset recipes
cur.execute("""
    SELECT count(distinct ri.ingredient_id) 
    FROM recipe_ingredients ri
    JOIN recipes r ON ri.recipe_id = r.id
    WHERE r.source_dataset = 'IndianFoodDataset'
""")
print("Distinct ingredients used by IndianFoodDataset:", cur.fetchone()[0])

# Check ingredients used ONLY by DataHive or showcase
cur.execute("""
    SELECT count(distinct ri.ingredient_id) 
    FROM recipe_ingredients ri
    JOIN recipes r ON ri.recipe_id = r.id
    WHERE r.source_dataset = 'DataHive' OR r.source_dataset IS NULL
""")
print("Distinct ingredients used by DataHive / Showcase:", cur.fetchone()[0])

# Check total ingredients
cur.execute("SELECT count(*) FROM ingredients")
print("Total ingredients in DB:", cur.fetchone()[0])

conn.close()
