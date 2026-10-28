import requests
import json

BASE_URL = "http://localhost:8080/api/v1"

def test_api():
    print("=" * 60)
    print("VERIFYING INCHEF LIVE BACKEND & AUTOCOMPLETE API")
    print("=" * 60)

    # 1. Test Autocomplete for "tom"
    print("\n1. Testing GET /ingredients/autocomplete?query=tom&limit=5...")
    r = requests.get(f"{BASE_URL}/ingredients/autocomplete", params={"query": "tom", "limit": 5})
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    data = r.json().get("data", [])
    print(f"   Returned {len(data)} items:")
    for it in data:
        print(f"   - ID {it['id']}: {it['name']} ({it.get('category')})")
    assert any(it['name'].lower() == 'tomato' for it in data), "Tomato not found in suggestions!"
    assert data[0]['name'].lower() == 'tomato', "Tomato should be ranked first!"

    # 2. Test Autocomplete for "chi"
    print("\n2. Testing GET /ingredients/autocomplete?query=chi&limit=5...")
    r = requests.get(f"{BASE_URL}/ingredients/autocomplete", params={"query": "chi", "limit": 5})
    assert r.status_code == 200
    data = r.json().get("data", [])
    print(f"   Returned {len(data)} items:")
    for it in data:
        print(f"   - ID {it['id']}: {it['name']} ({it.get('category')})")
    assert any("chi" in it['name'].lower() for it in data), "Prefix match failed!"

    # 3. Test Autocomplete for "paneer"
    print("\n3. Testing GET /ingredients/autocomplete?query=paneer&limit=5...")
    r = requests.get(f"{BASE_URL}/ingredients/autocomplete", params={"query": "paneer", "limit": 5})
    assert r.status_code == 200
    data = r.json().get("data", [])
    print(f"   Returned {len(data)} items:")
    for it in data:
        print(f"   - ID {it['id']}: {it['name']} ({it.get('category')})")
    assert data[0]['name'].lower() == 'paneer', "Paneer should be top result!"

    # 4. Test Direct Search for recipes
    print("\n4. Testing GET /recipes/search?query=Biryani...")
    r = requests.get(f"{BASE_URL}/recipes/search", params={"query": "Biryani", "size": 3})
    assert r.status_code == 200
    recipes_data = r.json().get("data", {}).get("content", [])
    print(f"   Returned {len(recipes_data)} recipes:")
    for rec in recipes_data:
        print(f"   - Recipe ID {rec['id']}: {rec['title']} ({rec.get('cuisine')})")
    assert len(recipes_data) > 0, "No recipes found for 'Biryani'!"

    # 5. Test Total Recipe Count from API
    print("\n5. Testing Total Recipes in Catalog via GET /recipes?size=1...")
    r = requests.get(f"{BASE_URL}/recipes", params={"size": 1})
    assert r.status_code == 200
    total_elements = r.json().get("data", {}).get("totalElements", 0)
    print(f"   Total Catalog Recipes: {total_elements}")
    assert total_elements == 6865, f"Expected exactly 6865 recipes, got {total_elements}!"

    # 6. Test Recommendation by Ingredients
    print("\n6. Testing POST /recommendations/by-ingredients...")
    payload = {
        "ingredients": ["Onion", "Tomato", "Paneer", "Garlic", "Ginger", "Cumin Seeds", "Turmeric"],
        "minIngredientMatch": 40.0,
        "limit": 5
    }
    r = requests.post(f"{BASE_URL}/recommendations/by-ingredients", json=payload)
    assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text}"
    rec_results = r.json().get("data", [])
    print(f"   Returned {len(rec_results)} recommendations:")
    for rec in rec_results[:3]:
        recipe_obj = rec.get('recipe', {})
        print(f"   - {recipe_obj.get('title')} (Match: {rec.get('ingredientMatchPercentage')}%, Score: {rec.get('matchScore')})")
    assert len(rec_results) > 0, "Expected recommendations for standard Indian ingredients!"

    print("\n" + "=" * 60)
    print("ALL API & WORKFLOW TESTS PASSED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    test_api()
