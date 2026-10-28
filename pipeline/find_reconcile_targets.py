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

cur.execute("""
    SELECT id, title, source_recipe_id FROM recipes 
    WHERE source_dataset = 'IndianFoodDataset' AND LOWER(title) LIKE '%pasta%'
    LIMIT 5
""")
print("Pasta in IndianFoodDataset:", cur.fetchall())

cur.execute("""
    SELECT id, title, source_recipe_id FROM recipes 
    WHERE source_dataset = 'IndianFoodDataset' AND LOWER(title) LIKE '%chicken biryani%'
    LIMIT 5
""")
print("Chicken Biryani in IndianFoodDataset:", cur.fetchall())

cur.execute("""
    SELECT id, title, source_recipe_id FROM recipes 
    WHERE source_dataset = 'IndianFoodDataset' AND LOWER(title) LIKE '%salad%' AND LOWER(title) LIKE '%chicken%'
    LIMIT 5
""")
print("Chicken Salad in IndianFoodDataset:", cur.fetchall())

conn.close()
