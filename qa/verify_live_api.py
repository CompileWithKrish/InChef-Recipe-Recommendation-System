#!/usr/bin/env python3
"""
verify_live_api.py - Comprehensive Live REST API Verification for InChef
Tests Workflow A (Direct Recipe Search), Workflow B (Ingredient Recommendations),
Recipe Details Retrieval, and Pantry Integration against localhost:8080.
"""

import sys
import json
import urllib.request
import urllib.parse

BASE_URL = "http://localhost:8080/api/v1"

def http_get(path):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode('utf-8'))

def http_post(path, data):
    url = f"{BASE_URL}{path}"
    payload = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json", "Accept": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode('utf-8'))

def main():
    print("=" * 70)
    print("INCHEF LIVE REST API VERIFICATION SUITE")
    print("=" * 70)
    passed = 0
    total = 0

    # -------------------------------------------------------------------------
    # 1. Workflow A: Direct Recipe Search
    # -------------------------------------------------------------------------
    print("\n[WORKFLOW A: DIRECT RECIPE SEARCH]")

    queries = ["Chicken Biryani", "Paneer Butter Masala", "Masala Dosa"]
    for q in queries:
        total += 1
        path = f"/recipes/search?query={urllib.parse.quote(q)}&page=0&size=5"
        status, res = http_get(path)
        content = res.get("data", {}).get("content", [])
        total_elements = res.get("data", {}).get("totalElements", 0)
        print(f" -> Search '{q}': Status={status}, Matched={total_elements} dishes")
        if status == 200 and total_elements > 0:
            top = content[0]
            print(f"    Top match: '{top.get('title')}' | ID: {top.get('id')} | Cuisine: {top.get('cuisine')}")
            passed += 1
        else:
            print(f"    FAIL: No matches for '{q}'")

    # Partial / Case-insensitive search
    total += 1
    status, res = http_get("/recipes/search?query=paneer&page=0&size=3")
    content = res.get("data", {}).get("content", [])
    print(f" -> Partial case-insensitive 'paneer': Status={status}, Count={len(content)}")
    if status == 200 and len(content) > 0:
        passed += 1
    else:
        print("    FAIL: Partial search returned 0 items")

    # -------------------------------------------------------------------------
    # 2. Recipe Detail Retrieval
    # -------------------------------------------------------------------------
    print("\n[RECIPE DETAIL RETRIEVAL]")
    total += 1
    # Pick first recipe from previous search
    first_recipe_id = content[0].get("id") if content else 1
    status, res = http_get(f"/recipes/{first_recipe_id}")
    recipe_data = res.get("data", {})
    print(f" -> GET /recipes/{first_recipe_id}: Title='{recipe_data.get('title')}'")
    print(f"    TranslatedName='{recipe_data.get('translatedRecipeName')}'")
    print(f"    Ingredients count: {len(recipe_data.get('ingredients', []))}")
    print(f"    Total Time: {recipe_data.get('totalTimeMinutes')} mins | Dietary: {recipe_data.get('dietaryType')}")
    if status == 200 and recipe_data.get("title") and len(recipe_data.get("ingredients", [])) > 0:
        passed += 1
    else:
        print("    FAIL: Recipe detail missing fields or ingredients")

    # -------------------------------------------------------------------------
    # 3. Workflow B: Ingredient-Based Recommendations
    # -------------------------------------------------------------------------
    print("\n[WORKFLOW B: INGREDIENT-BASED RECOMMENDATIONS]")

    # Test 3a: High match recommendations (Chicken, Rice, Onion, Garlic, Ginger, Tomato)
    total += 1
    req_data = {
        "ingredients": ["Chicken", "Rice", "Onion", "Garlic", "Ginger", "Tomato"],
        "minIngredientMatch": 75.0,
        "limit": 5
    }
    status, res = http_post("/recommendations/by-ingredients", req_data)
    recs = res.get("data", [])
    print(f" -> Recommend with Chicken, Rice, Onion...: Status={status}, Returned={len(recs)} recipes")
    all_ge_75 = True
    for r in recs:
        pct = r.get("ingredientMatchPercentage", 0)
        score = r.get("matchScore", 0)
        title = r.get("recipe", {}).get("title")
        print(f"    Match: {pct:.1f}% | Score: {score} | Recipe: '{title}'")
        if pct < 75.0:
            all_ge_75 = False
    if status == 200 and len(recs) > 0 and all_ge_75:
        passed += 1
    else:
        print(f"    FAIL: Recommendations below 75% or empty")

    # Test 3b: Substring false-positive protection: ["Pea"] must NOT match "Peanut"
    total += 1
    req_pea = {
        "ingredients": ["Pea"],
        "minIngredientMatch": 0.0,
        "limit": 10
    }
    status, res = http_post("/recommendations/by-ingredients", req_pea)
    recs_pea = res.get("data", [])
    has_peanut_false_positive = False
    for r in recs_pea:
        for matched in r.get("matchedIngredients", []):
            if "peanut" in matched.lower():
                has_peanut_false_positive = True
    print(f" -> False-positive check (Pea vs Peanut): Status={status}, FalsePositiveFound={has_peanut_false_positive}")
    if status == 200 and not has_peanut_false_positive:
        passed += 1
    else:
        print("    FAIL: Pea matched Peanut!")

    # Test 3c: Substring false-positive protection: ["Egg"] must NOT match "Eggplant"
    total += 1
    req_egg = {
        "ingredients": ["Egg"],
        "minIngredientMatch": 0.0,
        "limit": 10
    }
    status, res = http_post("/recommendations/by-ingredients", req_egg)
    recs_egg = res.get("data", [])
    has_eggplant_false_positive = False
    for r in recs_egg:
        for matched in r.get("matchedIngredients", []):
            if "eggplant" in matched.lower():
                has_eggplant_false_positive = True
    print(f" -> False-positive check (Egg vs Eggplant): Status={status}, FalsePositiveFound={has_eggplant_false_positive}")
    if status == 200 and not has_eggplant_false_positive:
        passed += 1
    else:
        print("    FAIL: Egg matched Eggplant!")

    # -------------------------------------------------------------------------
    # 4. Pantry & User Data Preservation
    # -------------------------------------------------------------------------
    print("\n[USER PANTRY & DATA PRESERVATION]")
    total += 1
    status, res = http_get("/users/1/pantry")
    pantry_items = res.get("data", [])
    print(f" -> GET /users/1/pantry (Alex Johnson): Status={status}, ItemCount={len(pantry_items)}")
    for item in pantry_items[:3]:
        print(f"    Item: {item.get('ingredientName')} | Qty: {item.get('quantity')} {item.get('unit')}")
    if status == 200 and len(pantry_items) > 0:
        passed += 1
    else:
        print("    FAIL: User pantry missing or empty")

    # -------------------------------------------------------------------------
    # Summary
    # -------------------------------------------------------------------------
    print("\n" + "=" * 70)
    print(f"VERIFICATION SUMMARY: {passed}/{total} tests passed ({passed/total*100:.1f}%)")
    print("=" * 70)
    if passed == total:
        print("ALL LIVE API VERIFICATION CHECKS COMPLETED SUCCESSFULLY!")
        return 0
    else:
        print("SOME CHECKS FAILED.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
