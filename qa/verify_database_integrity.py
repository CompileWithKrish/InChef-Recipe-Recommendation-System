import os
import pymysql
import json

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_USER = os.environ.get("DB_USERNAME", "root")
DB_PASS = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "inchef_db")

def verify_db():
    if not DB_PASS:
        print("Skipping DB integrity check: DB_PASSWORD environment variable is not set.")
        return
    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )
    
    report_lines = []
    report_lines.append("# Database Integrity & Verification Report")
    report_lines.append(f"**Database:** `{DB_NAME}` on `{DB_HOST}:3306`\n")
    
    with conn.cursor() as cursor:
        # 1. Recipe count
        cursor.execute("SELECT COUNT(*) AS cnt FROM recipes")
        recipe_count = cursor.fetchone()['cnt']
        
        # 2. Ingredient count
        cursor.execute("SELECT COUNT(*) AS cnt FROM ingredients")
        ingredient_count = cursor.fetchone()['cnt']
        
        # 3. RecipeIngredient count
        cursor.execute("SELECT COUNT(*) AS cnt FROM recipe_ingredients")
        ri_count = cursor.fetchone()['cnt']
        
        # 4. FK integrity / Orphans
        cursor.execute("""
            SELECT COUNT(*) AS cnt FROM recipe_ingredients ri
            LEFT JOIN recipes r ON ri.recipe_id = r.id
            WHERE r.id IS NULL
        """)
        orphan_recipe_links = cursor.fetchone()['cnt']
        
        cursor.execute("""
            SELECT COUNT(*) AS cnt FROM recipe_ingredients ri
            LEFT JOIN ingredients i ON ri.ingredient_id = i.id
            WHERE i.id IS NULL
        """)
        orphan_ingredient_links = cursor.fetchone()['cnt']
        
        # 5. Null required fields
        cursor.execute("SELECT COUNT(*) AS cnt FROM recipes WHERE title IS NULL OR title = ''")
        null_recipe_titles = cursor.fetchone()['cnt']
        
        cursor.execute("SELECT COUNT(*) AS cnt FROM ingredients WHERE name IS NULL OR name = ''")
        null_ingredient_names = cursor.fetchone()['cnt']
        
        cursor.execute("SELECT COUNT(*) AS cnt FROM recipes WHERE instructions IS NULL OR instructions = ''")
        null_instructions = cursor.fetchone()['cnt']
        
        # 6. Duplicate canonical ingredient names (case-insensitive)
        cursor.execute("""
            SELECT LOWER(name) AS lower_name, COUNT(*) AS cnt
            FROM ingredients
            GROUP BY LOWER(name)
            HAVING cnt > 1
        """)
        dup_ingredients = cursor.fetchall()
        
        # 7. Duplicate recipe titles
        cursor.execute("""
            SELECT LOWER(title) AS lower_title, COUNT(*) AS cnt
            FROM recipes
            GROUP BY LOWER(title)
            HAVING cnt > 1
        """)
        dup_recipe_titles = cursor.fetchall()
        
        # 8. Existing users
        cursor.execute("SELECT COUNT(*) AS cnt FROM users")
        user_count = cursor.fetchone()['cnt']
        cursor.execute("SELECT id, name, email, phone FROM users")
        users = cursor.fetchall()
        
        # 9. Existing favorites
        cursor.execute("SELECT COUNT(*) AS cnt FROM favorites")
        fav_count = cursor.fetchone()['cnt']
        
        # 10. Existing user pantry
        cursor.execute("SELECT COUNT(*) AS cnt FROM user_ingredients")
        pantry_count = cursor.fetchone()['cnt']
        
        # 11. Existing user preferences
        cursor.execute("SELECT COUNT(*) AS cnt FROM user_preferences")
        pref_count = cursor.fetchone()['cnt']
        
        # 12. Recommendation logs
        cursor.execute("SELECT COUNT(*) AS cnt FROM recommendation_logs")
        log_count = cursor.fetchone()['cnt']
        
        # 13. Obsolete datasets check
        cursor.execute("SELECT COUNT(*) AS cnt FROM recipes WHERE source_dataset != 'IndianFoodDataset' OR source_dataset IS NULL")
        obsolete_recipe_count = cursor.fetchone()['cnt']

        # Table of Summary
        report_lines.append("## 1. Table Counts & Baseline Verification\n")
        report_lines.append("| Entity / Table | Count | Expected Target | Status |")
        report_lines.append("| :--- | :--- | :--- | :--- |")
        report_lines.append(f"| Recipes (`recipes`) | {recipe_count:,} | Exactly 6,865 (>= 6,800) | {'PASS' if recipe_count >= 6800 and recipe_count <= 6900 else 'FAIL'} |")
        report_lines.append(f"| Obsolete Recipes (DataHive/Showcase) | {obsolete_recipe_count:,} | 0 (Purged) | {'PASS' if obsolete_recipe_count == 0 else 'FAIL'} |")
        report_lines.append(f"| Ingredients (`ingredients`) | {ingredient_count:,} | Clean Catalog (>= 2,500) | {'PASS' if ingredient_count >= 2500 else 'FAIL'} |")
        report_lines.append(f"| Recipe Ingredients (`recipe_ingredients`) | {ri_count:,} | Links (>= 70,000) | {'PASS' if ri_count >= 70000 else 'FAIL'} |")
        report_lines.append(f"| Users (`users`) | {user_count:,} | >= 3 | {'PASS' if user_count >= 3 else 'FAIL'} |")
        report_lines.append(f"| User Favorites (`favorites`) | {fav_count:,} | >= 6 | {'PASS' if fav_count >= 6 else 'FAIL'} |")
        report_lines.append(f"| User Pantry (`user_ingredients`) | {pantry_count:,} | >= 22 | {'PASS' if pantry_count >= 22 else 'FAIL'} |")
        report_lines.append(f"| User Preferences (`user_preferences`) | {pref_count:,} | >= 3 | {'PASS' if pref_count >= 3 else 'FAIL'} |")
        report_lines.append(f"| Recommendation Logs (`recommendation_logs`) | {log_count:,} | >= 0 | PASS |\n")
        
        report_lines.append("## 2. Relational & Referential Integrity\n")
        report_lines.append("| Check | Violations Found | Status |")
        report_lines.append("| :--- | :--- | :--- |")
        report_lines.append(f"| Orphan Recipe Links (`recipe_id` not in `recipes`) | {orphan_recipe_links} | {'PASS' if orphan_recipe_links == 0 else 'FAIL'} |")
        report_lines.append(f"| Orphan Ingredient Links (`ingredient_id` not in `ingredients`) | {orphan_ingredient_links} | {'PASS' if orphan_ingredient_links == 0 else 'FAIL'} |")
        report_lines.append(f"| Null / Empty Recipe Titles | {null_recipe_titles} | {'PASS' if null_recipe_titles == 0 else 'FAIL'} |")
        report_lines.append(f"| Null / Empty Ingredient Names | {null_ingredient_names} | {'PASS' if null_ingredient_names == 0 else 'FAIL'} |")
        report_lines.append(f"| Duplicate Canonical Ingredient Names (Case-Insensitive) | {len(dup_ingredients)} | {'PASS' if len(dup_ingredients) == 0 else 'FAIL'} |")
        report_lines.append(f"| Duplicate Recipe Titles | {len(dup_recipe_titles)} | {'PASS' if len(dup_recipe_titles) == 0 else 'INFO'} |\n")
        
        report_lines.append("## 3. Preserved User Profiles\n")
        report_lines.append("| ID | Name | Email | Phone |")
        report_lines.append("| :--- | :--- | :--- | :--- |")
        for u in users:
            report_lines.append(f"| {u['id']} | `{u['name']}` | `{u['email']}` | {u.get('phone') or 'N/A'} |")
            
        report_lines.append("\n## 4. Integrity Summary")
        is_clean = (recipe_count >= 6800 and obsolete_recipe_count == 0 and ingredient_count >= 2500 and ri_count >= 70000 and
                    orphan_recipe_links == 0 and orphan_ingredient_links == 0 and
                    null_recipe_titles == 0 and null_ingredient_names == 0 and len(dup_ingredients) == 0)
        report_lines.append(f"\n**Overall Database Integrity Status:** `{'PASSED - 100% CLEAN' if is_clean else 'FAILED'}`\n")
        
    os.makedirs("qa", exist_ok=True)
    with open("qa/database_integrity_report.md", "w", encoding="utf-8") as f:
        f.write("\n".join(report_lines))
    print(f"Generated qa/database_integrity_report.md successfully. Status: {'PASS' if is_clean else 'FAIL'}")

if __name__ == "__main__":
    verify_db()
