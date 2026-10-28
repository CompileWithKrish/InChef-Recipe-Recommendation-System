#!/usr/bin/env python3
"""
audit_comprehensive_runner.py
Executes comprehensive end-to-end audit test cases against the live InChef backend (port 8080)
and the MySQL database.
"""

import os
import sys
import json
import urllib.request
import urllib.parse
import urllib.error
import http.cookiejar
import pymysql

BASE_URL = "http://localhost:8080/api/v1"
DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "port": int(os.environ.get("DB_PORT", 3306)),
    "user": os.environ.get("DB_USERNAME", "root"),
    "password": os.environ.get("DB_PASSWORD", ""),
    "database": os.environ.get("DB_NAME", "inchef_db"),
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor
}

cookie_jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookie_jar))

def api_request(method, path, data=None, token=None):
    url = f"{BASE_URL}{path}"
    headers = {"Accept": "application/json"}
    payload = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        payload = json.dumps(data).encode("utf-8")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with opener.open(req) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            parsed = json.loads(body)
        except Exception:
            parsed = {"raw": body}
        return e.code, parsed
    except Exception as e:
        return 0, {"error": str(e)}

def db_query(sql, params=None):
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            cursor.execute(sql, params or ())
            return cursor.fetchall()
    finally:
        conn.close()

def run_audit():
    results = {}
    print("=" * 80)
    print("STARTING INCHEF RETRIEVAL SYSTEM COMPREHENSIVE AUDIT")
    print("=" * 80)

    # ---------------------------------------------------------
    # 1. DIRECT RECIPE SEARCH AUDIT
    # ---------------------------------------------------------
    print("\n--- 1. AUDITING DIRECT RECIPE SEARCH ---")
    s1 = {}
    
    # 1.1 Exact & Common Dish Names
    for dish in ["Chicken Biryani", "Paneer Butter Masala", "Masala Dosa", "paneer"]:
        status, res = api_request("GET", f"/recipes/search?query={urllib.parse.quote(dish)}&page=0&size=5")
        data = res.get("data", {})
        total = data.get("totalElements", 0)
        content = data.get("content", [])
        top_title = content[0]["title"] if content else None
        s1[f"search_{dish}"] = {
            "status_code": status,
            "total_matches": total,
            "top_result": top_title,
            "sample_results": [c["title"] for c in content[:3]]
        }
        print(f"  [Direct Search] '{dish}': status={status}, total={total}, top='{top_title}'")

    # 1.2 Case-Insensitive Test
    status_lower, res_lower = api_request("GET", "/recipes/search?query=chicken%20biryani")
    status_upper, res_upper = api_request("GET", "/recipes/search?query=CHICKEN%20BIRYANI")
    status_mixed, res_mixed = api_request("GET", "/recipes/search?query=ChIcKeN%20bIrYaNi")
    cnt_lower = res_lower.get("data", {}).get("totalElements", 0)
    cnt_upper = res_upper.get("data", {}).get("totalElements", 0)
    cnt_mixed = res_mixed.get("data", {}).get("totalElements", 0)
    s1["case_insensitivity"] = {
        "lower_count": cnt_lower,
        "upper_count": cnt_upper,
        "mixed_count": cnt_mixed,
        "consistent": cnt_lower == cnt_upper == cnt_mixed and cnt_lower > 0
    }
    print(f"  [Case-Insensitive] lower={cnt_lower}, upper={cnt_upper}, mixed={cnt_mixed}, consistent={s1['case_insensitivity']['consistent']}")

    # 1.3 Translated Title Searches
    # Query database for a recipe that has distinct translatedRecipeName
    trans_rows = db_query("SELECT id, title, translated_recipe_name FROM recipes WHERE translated_recipe_name IS NOT NULL AND translated_recipe_name != title AND is_published=1 LIMIT 1")
    if trans_rows:
        sample_trans = trans_rows[0]
        # Test searching by translated title
        t_term = sample_trans["translated_recipe_name"]
        status, res = api_request("GET", f"/recipes/search?query={urllib.parse.quote(t_term)}&page=0&size=5")
        found = any(r["id"] == sample_trans["id"] for r in res.get("data", {}).get("content", []))
        s1["translated_title_search"] = {
            "search_term": t_term,
            "target_recipe_id": sample_trans["id"],
            "target_recipe_title": sample_trans["title"],
            "status_code": status,
            "found_target": found
        }
        print(f"  [Translated Search] term='{t_term[:30]}...': status={status}, found_target={found}")

    # 1.4 Non-existent Dish Search
    status, res = api_request("GET", "/recipes/search?query=xyznonexistentdish123456789")
    total_non = res.get("data", {}).get("totalElements", -1)
    s1["non_existent_dish"] = {"status_code": status, "total_matches": total_non}
    print(f"  [Nonexistent Search] status={status}, matches={total_non}")

    # 1.5 Empty query & "all" handling
    status_empty, res_empty = api_request("GET", "/recipes/search?query=")
    total_empty = res_empty.get("data", {}).get("totalElements", 0)
    status_all, res_all = api_request("GET", "/recipes/search?query=all")
    total_all = res_all.get("data", {}).get("totalElements", 0)
    s1["empty_and_all_query"] = {
        "empty_status": status_empty,
        "empty_matches": total_empty,
        "all_status": status_all,
        "all_matches": total_all
    }
    print(f"  [Empty & All Query] empty_matches={total_empty}, all_matches={total_all}")

    # 1.6 Special characters
    status_spec, res_spec = api_request("GET", "/recipes/search?query=%21%40%23%24%25%5E%26%2A%28%29")
    s1["special_characters"] = {
        "status_code": status_spec,
        "total_matches": res_spec.get("data", {}).get("totalElements", 0)
    }
    print(f"  [Special Characters] status={status_spec}, matches={s1['special_characters']['total_matches']}")

    # 1.7 Pagination and Sorting
    status_p0, res_p0 = api_request("GET", "/recipes/search?query=Biryani&page=0&size=2")
    status_p1, res_p1 = api_request("GET", "/recipes/search?query=Biryani&page=1&size=2")
    p0_ids = [r["id"] for r in res_p0.get("data", {}).get("content", [])]
    p1_ids = [r["id"] for r in res_p1.get("data", {}).get("content", [])]
    # Sorting by totalTimeMinutes
    status_s_asc, res_s_asc = api_request("GET", "/recipes/search?query=Biryani&sort=totalTimeMinutes,asc&size=5")
    status_s_desc, res_s_desc = api_request("GET", "/recipes/search?query=Biryani&sort=totalTimeMinutes,desc&size=5")
    times_asc = [r["totalTimeMinutes"] for r in res_s_asc.get("data", {}).get("content", [])]
    times_desc = [r["totalTimeMinutes"] for r in res_s_desc.get("data", {}).get("content", [])]
    s1["pagination_and_sorting"] = {
        "p0_ids": p0_ids,
        "p1_ids": p1_ids,
        "pages_disjoint": len(set(p0_ids).intersection(set(p1_ids))) == 0 if p0_ids and p1_ids else False,
        "times_asc": times_asc,
        "times_desc": times_desc
    }
    print(f"  [Pagination] p0={p0_ids}, p1={p1_ids}, disjoint={s1['pagination_and_sorting']['pages_disjoint']}")
    print(f"  [Sorting] asc={times_asc}, desc={times_desc}")

    results["section1_direct_search"] = s1

    # ---------------------------------------------------------
    # 2. INGREDIENT-BASED RETRIEVAL & NORMALIZATION AUDIT
    # ---------------------------------------------------------
    print("\n--- 2. AUDITING INGREDIENT-BASED RETRIEVAL & ALIASES ---")
    s2 = {}

    # 2.1 Trimming, deduplication, case normalization
    req_norm = {
        "ingredients": ["  Chicken  ", "chicken", "CHICKEN", "  Onion ", "onion"],
        "minIngredientMatch": 0.0,
        "limit": 3
    }
    status, res = api_request("POST", "/recommendations/by-ingredients", req_norm)
    recs = res.get("data", [])
    # Verify deduplication did not produce multiple Chicken entries in matchedIngredients
    first_rec = recs[0] if recs else {}
    matched = first_rec.get("matchedIngredients", [])
    s2["normalization_and_dedup"] = {
        "status_code": status,
        "matched_ingredients": matched,
        "unique_matched_count": len(matched),
        "has_duplicates": len(matched) != len(set(matched))
    }
    print(f"  [Normalization & Dedup] status={status}, matched={matched}, has_duplicates={s2['normalization_and_dedup']['has_duplicates']}")

    # 2.2 Known Culinary Aliases
    aliases_to_test = [
        {"input": ["dahi"], "expected_canonical": "yogurt"},
        {"input": ["curd"], "expected_canonical": "yogurt"},
        {"input": ["hari mirch"], "expected_canonical": "green chilli"},
        {"input": ["green chili"], "expected_canonical": "green chilli"},
        {"input": ["dhania"], "expected_canonical": "coriander"},
        {"input": ["cilantro"], "expected_canonical": "coriander"},
        {"input": ["scallion"], "expected_canonical": "green onion"},
        {"input": ["spring onion"], "expected_canonical": "green onion"},
        {"input": ["jeera"], "expected_canonical": "cumin"},
        {"input": ["haldi"], "expected_canonical": "turmeric"},
        {"input": ["adrak"], "expected_canonical": "ginger"},
        {"input": ["lahsun"], "expected_canonical": "garlic"},
        {"input": ["pyaaz"], "expected_canonical": "onion"},
        {"input": ["tamatar"], "expected_canonical": "tomato"},
        {"input": ["shimla mirch"], "expected_canonical": "bell pepper"}
    ]
    alias_results = []
    for item in aliases_to_test:
        req_alias = {"ingredients": item["input"], "minIngredientMatch": 0.0, "limit": 5}
        status, res = api_request("POST", "/recommendations/by-ingredients", req_alias)
        matched_any = False
        recs = res.get("data", [])
        canonical_found = []
        for r in recs:
            for m in r.get("matchedIngredients", []):
                canonical_found.append(m)
                if item["expected_canonical"] in m.lower():
                    matched_any = True
        alias_results.append({
            "input": item["input"],
            "expected": item["expected_canonical"],
            "status": status,
            "matched_expected": matched_any,
            "sample_matched": canonical_found[:3]
        })
        print(f"  [Alias] {item['input']} -> expected '{item['expected_canonical']}': matched={matched_any}")
    s2["alias_handling"] = alias_results

    # 2.3 Unknown Ingredients
    req_unknown = {"ingredients": ["QuantumPlutoniumSpiceX99", "UnobtainiumRoot"], "minIngredientMatch": 10.0}
    status, res = api_request("POST", "/recommendations/by-ingredients", req_unknown)
    s2["unknown_ingredients"] = {
        "status_code": status,
        "returned_count": len(res.get("data", []))
    }
    print(f"  [Unknown Ingredients] status={status}, returned={s2['unknown_ingredients']['returned_count']}")

    # 2.4 False Positive Prevention: Word boundary & exact token testing
    fp_tests = [
        {"input": "pea", "forbidden": "peanut"},
        {"input": "peas", "forbidden": "peanut"},
        {"input": "egg", "forbidden": "eggplant"},
        {"input": "eggs", "forbidden": "eggplant"},
        {"input": "ice", "forbidden": "rice"},
        {"input": "corn", "forbidden": "peppercorn"}
    ]
    fp_results = []
    for test in fp_tests:
        req_fp = {"ingredients": [test["input"]], "minIngredientMatch": 0.0, "limit": 20}
        status, res = api_request("POST", "/recommendations/by-ingredients", req_fp)
        false_positive_found = False
        recs = res.get("data", [])
        for r in recs:
            for m in r.get("matchedIngredients", []):
                if test["forbidden"] in m.lower():
                    false_positive_found = True
        fp_results.append({
            "input": test["input"],
            "forbidden": test["forbidden"],
            "false_positive_found": false_positive_found,
            "total_evaluated": len(recs)
        })
        print(f"  [False Positive Check] '{test['input']}' vs forbidden '{test['forbidden']}': FalsePositiveFound={false_positive_found}")
    s2["false_positive_prevention"] = fp_results

    results["section2_ingredient_retrieval"] = s2

    # ---------------------------------------------------------
    # 3. VERIFY THE 75% THRESHOLD
    # ---------------------------------------------------------
    print("\n--- 3. AUDITING 75% MINIMUM MATCH THRESHOLD ---")
    s3 = {}
    
    # 3.1 Check default threshold in Spring Boot environment
    # Default is 75.0%
    # Test a query with 4 ingredients against a known recipe requiring 4 ingredients
    # Let's find recipes with exactly 4 ingredients in MySQL
    four_ing_recipes = db_query("""
        SELECT r.id, r.title, COUNT(ri.id) as ing_count
        FROM recipes r
        JOIN recipe_ingredients ri ON r.id = ri.recipe_id
        WHERE r.is_published = 1
        GROUP BY r.id, r.title
        HAVING ing_count = 4
        LIMIT 5
    """)
    s3["available_4_ingredient_recipes"] = four_ing_recipes
    
    if four_ing_recipes:
        target_recipe = four_ing_recipes[0]
        # Get its exact ingredients
        recipe_ings = db_query("""
            SELECT i.name FROM recipe_ingredients ri
            JOIN ingredients i ON ri.ingredient_id = i.id
            WHERE ri.recipe_id = %s
        """, (target_recipe["id"],))
        ing_names = [row["name"] for row in recipe_ings]
        print(f"  Target 4-ingredient recipe ID={target_recipe['id']} '{target_recipe['title']}': {ing_names}")

        # Test Case 1: 4 of 4 (100%) -> MUST BE ELIGIBLE
        req_100 = {"ingredients": ing_names, "limit": 10}
        st_100, res_100 = api_request("POST", "/recommendations/by-ingredients", req_100)
        rec_100 = next((r for r in res_100.get("data", []) if r.get("recipe", {}).get("id") == target_recipe["id"]), None)
        
        # Test Case 2: 3 of 4 (75%) -> MUST BE ELIGIBLE
        req_75 = {"ingredients": ing_names[:3], "limit": 10}
        st_75, res_75 = api_request("POST", "/recommendations/by-ingredients", req_75)
        rec_75 = next((r for r in res_75.get("data", []) if r.get("recipe", {}).get("id") == target_recipe["id"]), None)
        
        # Test Case 3: 2 of 4 (50%) -> MUST BE EXCLUDED by default 75% threshold
        req_50 = {"ingredients": ing_names[:2], "limit": 10}
        st_50, res_50 = api_request("POST", "/recommendations/by-ingredients", req_50)
        rec_50 = next((r for r in res_50.get("data", []) if r.get("recipe", {}).get("id") == target_recipe["id"]), None)

        s3["target_recipe_id"] = target_recipe["id"]
        s3["target_recipe_title"] = target_recipe["title"]
        s3["case_4_of_4_100pct"] = {
            "eligible": rec_100 is not None,
            "match_pct": rec_100.get("ingredientMatchPercentage") if rec_100 else None,
            "score": rec_100.get("matchScore") if rec_100 else None
        }
        s3["case_3_of_4_75pct"] = {
            "eligible": rec_75 is not None,
            "match_pct": rec_75.get("ingredientMatchPercentage") if rec_75 else None,
            "score": rec_75.get("matchScore") if rec_75 else None
        }
        s3["case_2_of_4_50pct"] = {
            "eligible": rec_50 is not None,
            "excluded_as_expected": rec_50 is None
        }
        print(f"  [Threshold 100%] eligible={s3['case_4_of_4_100pct']['eligible']}, pct={s3['case_4_of_4_100pct']['match_pct']}")
        print(f"  [Threshold 75%]  eligible={s3['case_3_of_4_75pct']['eligible']}, pct={s3['case_3_of_4_75pct']['match_pct']}")
        print(f"  [Threshold 50%]  excluded_as_expected={s3['case_2_of_4_50pct']['excluded_as_expected']}")

    # 3.2 Audit all returned recipes across arbitrary query to ensure >= 75%
    req_random = {"ingredients": ["Chicken", "Tomato", "Onion", "Garlic", "Ginger", "Rice"], "limit": 20}
    status, res = api_request("POST", "/recommendations/by-ingredients", req_random)
    all_recs = res.get("data", [])
    violations = [r for r in all_recs if r.get("ingredientMatchPercentage", 0) < 75.0]
    s3["general_threshold_compliance"] = {
        "status_code": status,
        "total_returned": len(all_recs),
        "violations_below_75pct": len(violations),
        "min_pct_observed": min([r.get("ingredientMatchPercentage", 0) for r in all_recs]) if all_recs else None
    }
    print(f"  [All Returned Recipes Threshold Check] count={len(all_recs)}, violations_below_75={len(violations)}")

    results["section3_threshold_audit"] = s3

    # ---------------------------------------------------------
    # 4. RANKING FORMULA & PERSONALIZATION FILTERS
    # ---------------------------------------------------------
    print("\n--- 4. AUDITING RANKING FORMULA & FILTERS ---")
    s4 = {}

    # 4.1 100-Point Scoring Breakdown Verification
    # Check ScoreBreakdown object in response
    if all_recs:
        sample = all_recs[0]
        sb = sample.get("scoreBreakdown", {})
        formula_check = {
            "ingredientScore": sb.get("ingredientScore"),
            "dietaryScore": sb.get("dietaryScore"),
            "cuisineScore": sb.get("cuisineScore"),
            "cookingTimeScore": sb.get("cookingTimeScore"),
            "categoryScore": sb.get("categoryScore"),
            "skillScore": sb.get("skillScore"),
            "totalScore": sb.get("totalScore"),
            "matchScore": sample.get("matchScore"),
            "reason": sample.get("reason"),
            "sum_matches_total": round(sb.get("ingredientScore", 0) + sb.get("dietaryScore", 0) + sb.get("cuisineScore", 0) + sb.get("cookingTimeScore", 0) + sb.get("categoryScore", 0), 1) == round(sb.get("totalScore", 0), 1)
        }
        s4["scoring_breakdown_audit"] = formula_check
        print(f"  [Scoring Breakdown] Ingr={sb.get('ingredientScore')}/60, Diet={sb.get('dietaryScore')}/15, Cuis={sb.get('cuisineScore')}/10, Time={sb.get('cookingTimeScore')}/10, Cat={sb.get('categoryScore')}/5 | Total={sb.get('totalScore')} | SumMatches={formula_check['sum_matches_total']}")
        print(f"  [Reason Explanation] '{sample.get('reason')}'")

    # 4.2 Deterministic Ordering Test
    # Verify recipes are sorted by matchScore desc, matchedIngredients.size desc, totalTimeMinutes asc, id asc
    req_det = {"ingredients": ["Tomato", "Onion", "Garlic", "Salt", "Ginger", "Chilli"], "minIngredientMatch": 50.0, "limit": 20}
    status, res = api_request("POST", "/recommendations/by-ingredients", req_det)
    det_recs = res.get("data", [])
    is_sorted = True
    for i in range(len(det_recs) - 1):
        c, n = det_recs[i], det_recs[i+1]
        c_score, n_score = c.get("matchScore", 0), n.get("matchScore", 0)
        c_matched, n_matched = len(c.get("matchedIngredients", [])), len(n.get("matchedIngredients", []))
        c_time = c.get("recipe", {}).get("totalTimeMinutes") or 999
        n_time = n.get("recipe", {}).get("totalTimeMinutes") or 999
        c_id = c.get("recipe", {}).get("id") or 0
        n_id = n.get("recipe", {}).get("id") or 0
        if c_score < n_score:
            is_sorted = False
            break
        elif c_score == n_score:
            if c_matched < n_matched:
                is_sorted = False
                break
            elif c_matched == n_matched:
                if c_time > n_time:
                    is_sorted = False
                    break
    s4["deterministic_ordering"] = {"is_sorted": is_sorted, "count": len(det_recs)}
    print(f"  [Deterministic Ordering] count={len(det_recs)}, is_sorted={is_sorted}")

    # 4.3 Dietary Filters: Vegetarian, Non-Vegetarian, Vegan
    diet_tests = {}
    for dt in ["VEGETARIAN", "NON_VEGETARIAN", "VEGAN"]:
        req_d = {"ingredients": ["Tomato", "Onion", "Garlic", "Potato", "Rice", "Paneer", "Chicken"], "dietaryType": dt, "minIngredientMatch": 50.0, "limit": 10}
        status, res = api_request("POST", "/recommendations/by-ingredients", req_d)
        recs_d = res.get("data", [])
        violations = []
        for r in recs_d:
            rd = r.get("recipe", {}).get("dietaryType")
            if dt == "VEGETARIAN" and rd not in ["VEGETARIAN", "VEGAN"]:
                violations.append(rd)
            elif dt == "VEGAN" and rd != "VEGAN":
                violations.append(rd)
        diet_tests[dt] = {
            "status_code": status,
            "count": len(recs_d),
            "violations": violations,
            "compliant": len(violations) == 0
        }
        print(f"  [Dietary Filter {dt}] count={len(recs_d)}, compliant={diet_tests[dt]['compliant']}")
    s4["dietary_filter_tests"] = diet_tests

    # 4.4 Cuisine Filter: "North Indian" vs "North Indian Recipes" vs "All"
    c_tests = {}
    for c_input in ["North Indian", "North Indian Recipes", "South Indian", "All"]:
        req_c = {"ingredients": ["Tomato", "Onion", "Garlic", "Salt", "Ginger", "Chilli", "Rice", "Flour"], "cuisine": c_input, "minIngredientMatch": 40.0, "limit": 10}
        status, res = api_request("POST", "/recommendations/by-ingredients", req_c)
        recs_c = res.get("data", [])
        c_tests[c_input] = {
            "status_code": status,
            "count": len(recs_c),
            "cuisines_observed": list(set([r.get("recipe", {}).get("cuisine") for r in recs_c]))
        }
        print(f"  [Cuisine Filter '{c_input}'] count={len(recs_c)}, observed={c_tests[c_input]['cuisines_observed'][:2]}")
    s4["cuisine_filter_tests"] = c_tests

    # 4.5 Max Cooking Time Filter
    time_tests = {}
    for max_t in [20, 30, 45]:
        req_t = {"ingredients": ["Tomato", "Onion", "Garlic", "Salt", "Ginger", "Rice"], "maxCookingTime": max_t, "minIngredientMatch": 40.0, "limit": 10}
        status, res = api_request("POST", "/recommendations/by-ingredients", req_t)
        recs_t = res.get("data", [])
        times = [r.get("recipe", {}).get("totalTimeMinutes") for r in recs_t if r.get("recipe", {}).get("totalTimeMinutes") is not None]
        viol = [t for t in times if t > max_t]
        time_tests[f"max_{max_t}"] = {
            "status_code": status,
            "count": len(recs_t),
            "max_time_observed": max(times) if times else None,
            "violations": viol,
            "compliant": len(viol) == 0
        }
        print(f"  [Cooking Time Filter <= {max_t} mins] count={len(recs_t)}, max_observed={time_tests[f'max_{max_t}']['max_time_observed']}, compliant={time_tests[f'max_{max_t}']['compliant']}")
    s4["time_filter_tests"] = time_tests

    # 4.6 Allergy Restrictions: Non-bypassable constraint
    # Alex Johnson (user 1) preferences check
    user1_pref = db_query("SELECT * FROM user_preferences WHERE user_id = 1")
    s4["user1_preferences_in_db"] = user1_pref[0] if user1_pref else None
    print(f"  [User 1 Preferences in MySQL] allergies={user1_pref[0].get('allergies') if user1_pref else 'None'}")

    results["section4_ranking_and_filters"] = s4

    # ---------------------------------------------------------
    # 5. PANTRY-BASED RETRIEVAL & SECURITY AUDIT
    # ---------------------------------------------------------
    print("\n--- 5. AUDITING PANTRY-BASED RETRIEVAL & SECURITY ---")
    s5 = {}

    # 5.1 Test unauthenticated request to /users/1/pantry
    st_anon_pantry, res_anon_pantry = api_request("GET", "/users/1/pantry")
    s5["anonymous_pantry_access"] = {
        "status_code": st_anon_pantry,
        "is_unauthorized": st_anon_pantry == 401
    }
    print(f"  [Security] Anonymous GET /users/1/pantry: status={st_anon_pantry} (expected 401)")

    # 5.2 Authenticate user 1 (Alex Johnson) or test with valid login
    # Check users table in MySQL
    users_db = db_query("SELECT id, name, email FROM users")
    s5["users_in_db"] = users_db
    print(f"  [Database Users] Total={len(users_db)}")

    # Try logging in with test user or registered user
    login_status, login_res = api_request("POST", "/auth/login", {"email": "alex@inchef.com", "password": "password123"})
    if login_status != 200:
        # Register a dedicated audit user to test auth & cross-user pantry
        import time
        timestamp = int(time.time())
        audit_email = f"audit_chef_{timestamp}@inchef.com"
        reg_status, reg_res = api_request("POST", "/auth/register", {
            "name": "Audit Chef",
            "email": audit_email,
            "password": "Password@123"
        })
        print(f"  [Auth Registration] email={audit_email}, status={reg_status}")
        login_status, login_res = api_request("POST", "/auth/login", {
            "email": audit_email,
            "password": "Password@123"
        })
    
    user_id = login_res.get("data", {}).get("id")
    print(f"  [Auth Login] status={login_status}, user_id={user_id}")
    s5["audit_user_auth"] = {"login_status": login_status, "user_id": user_id}

    if user_id:
        # 5.3 Test adding items to pantry
        st_add, res_add = api_request("POST", f"/users/{user_id}/pantry", {
            "ingredientName": "Basmati Rice",
            "category": "Grains",
            "quantity": 2.0,
            "unit": "CUPS"
        })
        print(f"  [Pantry Add] status={st_add}")

        # Add second item
        api_request("POST", f"/users/{user_id}/pantry", {
            "ingredientName": "Chicken",
            "category": "Proteins",
            "quantity": 500.0,
            "unit": "GRAMS"
        })
        api_request("POST", f"/users/{user_id}/pantry", {
            "ingredientName": "Onion",
            "category": "Vegetables",
            "quantity": 3.0,
            "unit": "PIECES"
        })

        # 5.4 Test GET pantry
        st_get_p, res_get_p = api_request("GET", f"/users/{user_id}/pantry")
        pantry_items = res_get_p.get("data", [])
        s5["pantry_retrieval"] = {
            "status_code": st_get_p,
            "items_count": len(pantry_items),
            "items": [f"{i.get('ingredientName')} ({i.get('quantity')} {i.get('unit')})" for i in pantry_items]
        }
        print(f"  [Pantry Retrieval] count={len(pantry_items)}, items={s5['pantry_retrieval']['items']}")

        # 5.5 Test recommendation with usePantry=true
        req_pantry_rec = {
            "userId": user_id,
            "usePantry": True,
            "ingredients": ["Garlic", "Ginger"],
            "minIngredientMatch": 50.0,
            "limit": 5
        }
        st_prec, res_prec = api_request("POST", "/recommendations/by-ingredients", req_pantry_rec)
        p_recs = res_prec.get("data", [])
        s5["pantry_recommendation"] = {
            "status_code": st_prec,
            "recs_count": len(p_recs),
            "sample_recs": [r.get("recipe", {}).get("title") for r in p_recs[:3]],
            "sample_matched": p_recs[0].get("matchedIngredients") if p_recs else []
        }
        print(f"  [Recommendation usePantry=true] status={st_prec}, count={len(p_recs)}, sample_matched={s5['pantry_recommendation']['sample_matched']}")

        # 5.6 Cross-user isolation test: Try to access user 1's pantry with audit user's token
        st_cross, res_cross = api_request("GET", "/users/1/pantry")
        s5["cross_user_access"] = {
            "target_user_id": 1,
            "calling_user_id": user_id,
            "status_code": st_cross,
            "is_forbidden": st_cross == 403
        }
        print(f"  [Security] Cross-user GET /users/1/pantry: status={st_cross} (expected 403 Forbidden)")

    results["section5_pantry_and_security"] = s5

    # ---------------------------------------------------------
    # SAVE FULL AUDIT RESULTS
    # ---------------------------------------------------------
    with open("qa/audit_comprehensive_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, default=str)
    print("\nSaved full audit results to qa/audit_comprehensive_results.json")
    print("=" * 80)

if __name__ == "__main__":
    run_audit()
