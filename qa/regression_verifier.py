import requests
import json

BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api/v1"

def log(msg, status="INFO"):
    print(f"[{status}] {msg}")

def test_recommendation_course_filter():
    log("=== Testing Course / MealType Filter on Live Endpoint ===")
    
    # 1. Unfiltered recommendations
    payload_no_course = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "minIngredientMatch": 20.0,
        "limit": 10
    }
    r1 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_no_course)
    assert r1.status_code == 200, f"Expected 200, got {r1.status_code}: {r1.text}"
    data1 = r1.json()["data"]
    log(f"Unfiltered recommendations count: {len(data1)}")
    assert len(data1) > 0, "Expected recommendations"
    
    # Check structure
    for item in data1:
        recipe = item.get("recipe")
        assert recipe is not None, "Missing nested recipe object in response"
        rec_id = recipe.get("id")
        title = recipe.get("title")
        assert rec_id is not None and rec_id > 0, f"Invalid recipe ID: {rec_id}"
        assert title is not None and len(title) > 0, f"Invalid recipe title: {title}"
        
        # Match percentage checks
        match_pct = item.get("ingredientMatchPercentage")
        match_score = item.get("matchScore")
        assert match_pct is not None and 20.0 <= match_pct <= 100.0, f"Invalid match percentage: {match_pct}"
        assert match_score is not None and 0 <= match_score <= 100, f"Invalid match score: {match_score}"
        log(f"Verified recipe #{rec_id} '{title}' - Meal: {recipe.get('mealType')} - MatchPct: {match_pct}% - Score: {match_score}")
    
    # 2. DINNER filter
    payload_dinner = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "mealType": "DINNER",
        "minIngredientMatch": 20.0,
        "limit": 10
    }
    r2 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_dinner)
    assert r2.status_code == 200
    data2 = r2.json()["data"]
    log(f"DINNER filtered recommendations count: {len(data2)}")
    assert len(data2) > 0, "Expected DINNER recommendations"
    for item in data2:
        m_type = item["recipe"]["mealType"]
        assert m_type == "DINNER", f"Expected DINNER meal type, got {m_type}"
    log("DINNER filter verified: 100% of returned recipes are DINNER.")

    # 3. BREAKFAST filter
    payload_breakfast = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "mealType": "BREAKFAST",
        "minIngredientMatch": 20.0,
        "limit": 10
    }
    r3 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_breakfast)
    assert r3.status_code == 200
    data3 = r3.json()["data"]
    log(f"BREAKFAST filtered recommendations count: {len(data3)}")
    assert len(data3) > 0, "Expected BREAKFAST recommendations"
    for item in data3:
        m_type = item["recipe"]["mealType"]
        assert m_type == "BREAKFAST", f"Expected BREAKFAST meal type, got {m_type}"
    log("BREAKFAST filter verified: 100% of returned recipes are BREAKFAST.")

    # 4. LUNCH filter
    payload_lunch = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "mealType": "LUNCH",
        "minIngredientMatch": 20.0,
        "limit": 10
    }
    r4 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_lunch)
    assert r4.status_code == 200
    data4 = r4.json()["data"]
    log(f"LUNCH filtered recommendations count: {len(data4)}")
    assert len(data4) > 0, "Expected LUNCH recommendations"
    for item in data4:
        m_type = item["recipe"]["mealType"]
        assert m_type == "LUNCH", f"Expected LUNCH meal type, got {m_type}"
    log("LUNCH filter verified: 100% of returned recipes are LUNCH.")

    # 5. Course filter set to 'All' or null -> unrestricted
    payload_all_course = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "course": "All",
        "minIngredientMatch": 20.0,
        "limit": 10
    }
    r5 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_all_course)
    assert r5.status_code == 200
    data5 = r5.json()["data"]
    assert len(data5) == len(data1), f"Expected same count as unfiltered ({len(data1)}), got {len(data5)}"
    assert [x['recipe']['id'] for x in data5] == [x['recipe']['id'] for x in data1], "Expected identical ordering"
    log("Course 'All' verified: returned unrestricted results matching unfiltered query.")

    # 6. Default 75% threshold test
    payload_75 = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "limit": 10
    }
    r6 = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload_75)
    assert r6.status_code == 200
    data6 = r6.json()["data"]
    for item in data6:
        assert item["ingredientMatchPercentage"] >= 75.0, f"Expected >= 75%, got {item['ingredientMatchPercentage']}%"
    log(f"Default 75% threshold verified: all {len(data6)} returned recipes have match >= 75.0%.")

def test_recipe_details_navigation():
    log("=== Testing Recipe Details Navigation From Recommendation ===")
    payload = {
        "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
        "minIngredientMatch": 20.0,
        "limit": 3
    }
    r = requests.post(f"{API_BASE}/recommendations/by-ingredients", json=payload)
    assert r.status_code == 200
    items = r.json()["data"]
    for item in items:
        rec_id = item["recipe"]["id"]
        # Fetch recipe details
        det_res = requests.get(f"{API_BASE}/recipes/{rec_id}")
        assert det_res.status_code == 200, f"Failed to fetch recipe #{rec_id}: {det_res.status_code}"
        det = det_res.json()["data"]
        assert det["id"] == rec_id
        assert det["title"] == item["recipe"]["title"]
        log(f"Recipe #{rec_id} '{det['title']}' loaded cleanly: {len(det.get('ingredients', []))} ingredients, {len(det.get('instructions', []))} steps.")

def test_full_regression_pipeline():
    test_recommendation_course_filter()
    test_recipe_details_navigation()
    log("=== ALL REGRESSION CHECKS PASSED ===", status="SUCCESS")

if __name__ == "__main__":
    test_full_regression_pipeline()
