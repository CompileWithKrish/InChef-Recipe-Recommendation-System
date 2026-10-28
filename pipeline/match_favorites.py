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
    SELECT f.id, f.user_id, f.recipe_id, r.title, r.source_dataset, r.source_recipe_id
    FROM favorites f
    JOIN recipes r ON f.recipe_id = r.id
""")
favs = cur.fetchall()

for f in favs:
    fav_id, u_id, r_id, title, s_ds, s_id = f
    print(f"\nFavorite ID {fav_id} (User {u_id}): '{title}' [Dataset: {s_ds}, SourceID: {s_id}]")
    if s_ds == 'IndianFoodDataset':
        print(f"  -> Already in IndianFoodDataset (Keep recipe ID {r_id})")
    else:
        # Search by keywords
        words = [w for w in title.replace('-', ' ').replace('(', ' ').replace(')', ' ').split() if len(w) > 3 and w.lower() not in ['recipe', 'style', 'classic', 'creamy', 'spicy', 'loaded']]
        query = "%" + "%".join(words[:2]) + "%" if words else "%"
        cur.execute("""
            SELECT id, title, source_recipe_id 
            FROM recipes 
            WHERE source_dataset = 'IndianFoodDataset' 
            AND (title LIKE %s OR translated_recipe_name LIKE %s)
            LIMIT 3
        """, (query, query))
        matches = cur.fetchall()
        if matches:
            print(f"  -> Best IndianFood matches for query '{query}':")
            for m in matches:
                print(f"     ID {m[0]}: {m[1]} ({m[2]})")
        else:
            # Fallback search single main word
            for w in words:
                cur.execute("""
                    SELECT id, title, source_recipe_id 
                    FROM recipes 
                    WHERE source_dataset = 'IndianFoodDataset' 
                    AND (title LIKE %s OR translated_recipe_name LIKE %s)
                    LIMIT 2
                """, ('%' + w + '%', '%' + w + '%'))
                sub_matches = cur.fetchall()
                if sub_matches:
                    print(f"  -> Partial matches for keyword '{w}':")
                    for m in sub_matches:
                        print(f"     ID {m[0]}: {m[1]} ({m[2]})")
                    break

conn.close()
