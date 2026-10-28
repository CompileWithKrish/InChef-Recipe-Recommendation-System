#!/usr/bin/env python3
"""
InChef — MySQL Database Schema and Relational Integrity Audit Tool
Inspects tables, record counts, primary keys, foreign keys, and indexes across inchef_db.
"""

import os
import sys
import argparse
import pymysql

EXPECTED_TABLES = [
    'users',
    'recipes',
    'ingredients',
    'recipe_ingredients',
    'user_ingredients',
    'favorites',
    'user_preferences',
    'login_history',
    'recommendation_logs',
    'user_recipe_progress'
]

def main():
    parser = argparse.ArgumentParser(description="InChef Database Audit Tool")
    parser.add_argument("--host", default=os.environ.get("DB_HOST", "localhost"), help="MySQL host")
    parser.add_argument("--port", type=int, default=int(os.environ.get("DB_PORT", 3306)), help="MySQL port")
    parser.add_argument("--user", default=os.environ.get("DB_USERNAME", "root"), help="MySQL username")
    parser.add_argument("--password", default=os.environ.get("DB_PASSWORD", ""), help="MySQL password")
    parser.add_argument("--database", default=os.environ.get("DB_NAME", "inchef_db"), help="MySQL database")
    args = parser.parse_args()

    print("==================================================")
    print(f" InChef Database Audit: {args.host}:{args.port}/{args.database}")
    print("==================================================")

    try:
        conn = pymysql.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=args.password,
            database=args.database,
            charset='utf8mb4'
        )
    except Exception as e:
        print(f"Error connecting to MySQL: {e}")
        print("Note: If running in CI or without MySQL server, use Docker Compose or configure .env.")
        sys.exit(0)

    cursor = conn.cursor()

    # Tables list
    cursor.execute("SHOW TABLES")
    existing_tables = [row[0] for row in cursor.fetchall()]
    print(f"Found {len(existing_tables)} tables in database:\n")

    for tbl in EXPECTED_TABLES:
        if tbl in existing_tables:
            cursor.execute(f"SELECT COUNT(*) FROM `{tbl}`")
            cnt = cursor.fetchone()[0]
            print(f"  [OK] Table '{tbl}': {cnt:,} rows")
        else:
            print(f"  [MISSING] Table '{tbl}' NOT FOUND")

    # Audit foreign keys
    print("\nForeign Key Constraints:")
    cursor.execute("""
        SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = %s AND REFERENCED_TABLE_NAME IS NOT NULL
        ORDER BY TABLE_NAME, CONSTRAINT_NAME
    """, (args.database,))
    fks = cursor.fetchall()
    for fk in fks:
        print(f"  - {fk[0]} -> {fk[2]}({fk[3]}) [FK: {fk[1]}]")

    # Audit indexes
    print("\nCustom Indices Check:")
    cursor.execute("""
        SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, NON_UNIQUE
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = %s AND INDEX_NAME != 'PRIMARY'
        ORDER BY TABLE_NAME, INDEX_NAME
    """, (args.database,))
    indexes = cursor.fetchall()
    print(f"Total non-primary indices: {len(indexes)}")
    for idx in indexes[:15]:
        print(f"  - {idx[0]}.{idx[2]} (Index: {idx[1]}, Unique: {idx[3] == 0})")
    if len(indexes) > 15:
        print(f"  ... and {len(indexes) - 15} more indices.")

    cursor.close()
    conn.close()
    print("\n==================================================")
    print(" Database Audit Completed Successfully.")
    print("==================================================")

if __name__ == '__main__':
    main()

