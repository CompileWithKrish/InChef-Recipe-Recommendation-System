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
    SELECT DISTINCT i.id, i.name 
    FROM user_ingredients ui
    JOIN ingredients i ON ui.ingredient_id = i.id
    WHERE i.id NOT IN (
        SELECT DISTINCT ri.ingredient_id 
        FROM recipe_ingredients ri
        JOIN recipes r ON ri.recipe_id = r.id
        WHERE r.source_dataset = 'IndianFoodDataset'
    )
""")
pantry_not_in_ifd = cur.fetchall()
print("Pantry ingredients not in IndianFoodDataset recipes:", pantry_not_in_ifd)

conn.close()
