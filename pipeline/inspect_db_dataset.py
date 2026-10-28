import os
import sys
import pymysql
import pandas as pd

excel_path = os.environ.get(
    "INCHEF_DATASET_PATH",
    os.path.join(os.path.dirname(__file__), "..", "data", "samples", "sample_recipes.xlsx")
)
if os.path.exists(excel_path):
    df = pd.read_excel(excel_path, sheet_name=0)
    print(f"Excel total rows: {len(df)}")
else:
    print(f"Excel file not found at {excel_path}. Set INCHEF_DATASET_PATH environment variable.")

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

cur.execute("SELECT count(*) FROM recipes WHERE source_dataset = 'IndianFoodDataset'")
print("IndianFoodDataset recipes in DB:", cur.fetchone()[0])

cur.execute("SELECT count(*) FROM recipes WHERE source_dataset = 'DataHive'")
print("DataHive recipes in DB:", cur.fetchone()[0])

cur.execute("SELECT count(*) FROM recipes WHERE source_dataset IS NULL")
print("Null source_dataset recipes in DB:", cur.fetchone()[0])

cur.execute("SELECT id, title, source_recipe_id FROM recipes WHERE source_dataset = 'IndianFoodDataset' LIMIT 5")
for r in cur.fetchall():
    print("Sample DB IndianFood row:", r)

conn.close()
