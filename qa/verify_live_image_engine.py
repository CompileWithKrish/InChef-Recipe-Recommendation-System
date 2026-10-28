import requests
import json

BASE_URL = "http://localhost:8080"
ADMIN_KEY = "InChefSecureAdminKey2026"

def test_endpoints():
    print("==================================================")
    print("TESTING INCHEF LIVE IMAGE RETRIEVAL ENGINE")
    print("==================================================")

    # 1. Fetch a recipe ID to test
    print("\n1. Fetching recipe listing to pick a test recipe...")
    r = requests.get(f"{BASE_URL}/api/v1/recipes?size=1")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    data = r.json()
    first_recipe = data["data"]["content"][0]
    recipe_id = first_recipe["id"]
    recipe_title = first_recipe["title"]
    print(f"   Picked Recipe ID {recipe_id}: '{recipe_title}'")
    print(f"   Current Image URL: {first_recipe.get('imageUrl')}")

    # 2. Test GET /api/v1/recipes/{id}/image
    print(f"\n2. Testing GET /api/v1/recipes/{recipe_id}/image (Public)...")
    r = requests.get(f"{BASE_URL}/api/v1/recipes/{recipe_id}/image")
    print(f"   Status Code: {r.status_code}")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    img_data = r.json()["data"]
    print(f"   Recipe Title: {img_data.get('title')}")
    print(f"   Image URL: {img_data.get('imageUrl')}")
    print(f"   Source URL: {img_data.get('sourceUrl')}")
    print(f"   Source Type: {img_data.get('imageSourceType')}")
    print(f"   Image Status: {img_data.get('imageStatus')}")

    # 3. Test Security: unauthorized access to admin endpoint
    print("\n3. Testing Security: Accessing /api/v1/admin/recipe-images/status without credentials...")
    r = requests.get(f"{BASE_URL}/api/v1/admin/recipe-images/status")
    print(f"   Status Code: {r.status_code}")
    assert r.status_code in [401, 403], f"Expected 401/403, got {r.status_code}"
    print("   [PASSED] Unauthenticated request safely rejected!")

    # 4. Test Authorized Admin Access with X-Admin-Key
    headers = {"X-Admin-Key": ADMIN_KEY}
    print("\n4. Testing GET /api/v1/admin/recipe-images/status with X-Admin-Key...")
    r = requests.get(f"{BASE_URL}/api/v1/admin/recipe-images/status", headers=headers)
    print(f"   Status Code: {r.status_code}")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    print(f"   Batch Status Response: {json.dumps(r.json()['data'], indent=2)}")

    # 5. Test GET /api/v1/admin/recipe-images/audit
    print("\n5. Testing GET /api/v1/admin/recipe-images/audit with X-Admin-Key...")
    r = requests.get(f"{BASE_URL}/api/v1/admin/recipe-images/audit", headers=headers)
    print(f"   Status Code: {r.status_code}")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    audit = r.json()["data"]
    print(f"   Total Recipes: {audit.get('totalRecipes'):,}")
    print(f"   Resolved Images: {audit.get('imagesExtractedFromMetadata'):,}")
    print(f"   Pending Recipes: {audit.get('pendingCount'):,}")
    print(f"   Unique Image URLs: {audit.get('uniqueNonemptyImageCount'):,}")
    print(f"   Repeated Image URLs: {audit.get('repeatedImageCount'):,}")

    # 6. Test Dry-run Batch Resolution
    print("\n6. Testing POST /api/v1/admin/recipe-images/resolve?limit=3&batchSize=3&dryRun=true...")
    r = requests.post(f"{BASE_URL}/api/v1/admin/recipe-images/resolve?limit=3&batchSize=3&dryRun=true", headers=headers)
    print(f"   Status Code: {r.status_code}")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}"
    batch_result = r.json()["data"]
    print(f"   DryRun Batch Result: {json.dumps(batch_result, indent=2)}")

    print("\n==================================================")
    print("ALL LIVE ENDPOINTS AND SECURITY CHECKS PASSED!")
    print("==================================================")

if __name__ == "__main__":
    test_endpoints()
