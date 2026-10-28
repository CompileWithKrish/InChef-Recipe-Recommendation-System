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
    charset='utf8mb4'
)

with conn.cursor() as cursor:
    for t in ['recipes', 'ingredients', 'recipe_ingredients']:
        print(f"\n--- Columns in {t} ---")
        cursor.execute(f"DESCRIBE `{t}`")
        for col in cursor.fetchall():
            print(f"  {col[0]}: {col[1]} (Null: {col[2]}, Key: {col[3]}, Default: {col[4]})")

conn.close()
