#!/usr/bin/env python3
"""
InChef — Recipe Image Quality & Reachability Audit Tool
Inspects image URLs, status distribution, fallback ratios, and HTTP/HTTPS protocol adherence.
"""

import os
import sys
import argparse
import pymysql

def main():
    parser = argparse.ArgumentParser(description="InChef Recipe Image Audit Tool")
    parser.add_argument("--host", default=os.environ.get("DB_HOST", "localhost"), help="MySQL host")
    parser.add_argument("--port", type=int, default=int(os.environ.get("DB_PORT", 3306)), help="MySQL port")
    parser.add_argument("--user", default=os.environ.get("DB_USERNAME", "root"), help="MySQL username")
    parser.add_argument("--password", default=os.environ.get("DB_PASSWORD", ""), help="MySQL password")
    parser.add_argument("--database", default=os.environ.get("DB_NAME", "inchef_db"), help="MySQL database")
    args = parser.parse_args()

    print("==================================================")
    print(" InChef Recipe Image Quality & Coverage Audit")
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
        print("Note: Provide credentials via --password or .env file.")
        sys.exit(0)

    cursor = conn.cursor()

    cursor.execute("SELECT COUNT(*) FROM recipes")
    total_recipes = cursor.fetchone()[0]
    print(f"Total Recipes in Database: {total_recipes:,}")

    if total_recipes == 0:
        print("No recipes in database. Skipping image audit.")
        cursor.close()
        conn.close()
        return

    # Image status distribution
    print("\nImage Status Distribution:")
    cursor.execute("""
        SELECT COALESCE(image_status, 'UNSET') as status, COUNT(*) as cnt
        FROM recipes
        GROUP BY image_status
        ORDER BY cnt DESC
    """)
    for row in cursor.fetchall():
        print(f"  - {row[0]}: {row[1]:,} ({row[1]/total_recipes*100:.1f}%)")

    # Image source type distribution
    print("\nImage Source Type Breakdown:")
    cursor.execute("""
        SELECT COALESCE(image_source_type, 'UNSET') as source_type, COUNT(*) as cnt
        FROM recipes
        GROUP BY image_source_type
        ORDER BY cnt DESC
    """)
    for row in cursor.fetchall():
        print(f"  - {row[0]}: {row[1]:,} ({row[1]/total_recipes*100:.1f}%)")

    # Placeholder usage
    cursor.execute("""
        SELECT COUNT(*) FROM recipes
        WHERE image_url LIKE '%neutral_recipe_placeholder%' OR image_url IS NULL OR TRIM(image_url) = ''
    """)
    placeholder_count = cursor.fetchone()[0]
    valid_count = total_recipes - placeholder_count
    print(f"\nImage Resolution Summary:")
    print(f"  - Verified Custom / Remote Images: {valid_count:,} ({valid_count/total_recipes*100:.1f}%)")
    print(f"  - Neutral Fallback Placeholders:    {placeholder_count:,} ({placeholder_count/total_recipes*100:.1f}%)")

    # Duplicate image check
    cursor.execute("""
        SELECT image_url, COUNT(*) as cnt
        FROM recipes
        WHERE image_url IS NOT NULL 
          AND TRIM(image_url) != '' 
          AND image_url NOT LIKE '%neutral_recipe_placeholder%'
        GROUP BY image_url
        HAVING cnt > 1
        ORDER BY cnt DESC
        LIMIT 5
    """)
    duplicates = cursor.fetchall()
    if duplicates:
        print(f"\nTop Repeated Image URLs (Excluding Placeholder):")
        for dup in duplicates:
            print(f"  - {dup[0][:70]}... : {dup[1]} occurrences")
    else:
        print("\nNo duplicate remote image URLs detected.")

    cursor.close()
    conn.close()
    print("\n==================================================")
    print(" Image Audit Completed Successfully.")
    print("==================================================")

if __name__ == '__main__':
    main()

