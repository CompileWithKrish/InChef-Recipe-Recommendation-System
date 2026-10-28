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
    cursor.execute("SHOW TABLES")
    tables = cursor.fetchall()
    print("Tables in inchef_db:")
    for t in tables:
        tname = t[0]
        cursor.execute(f"SELECT COUNT(*) FROM `{tname}`")
        cnt = cursor.fetchone()[0]
        print(f"  {tname}: {cnt} rows")

conn.close()
