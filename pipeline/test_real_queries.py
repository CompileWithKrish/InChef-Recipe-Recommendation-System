import urllib.request
import urllib.parse
import json

BASE_URL = "http://localhost:8080/api/v1"

def http_get(endpoint, params=None):
    url = f"{BASE_URL}/{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

def http_post(endpoint, data):
    url = f"{BASE_URL}/{endpoint}"
    req = urllib.request.Request(
        url,
        data=json.dumps(data).encode('utf-8'),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

results = {}

print("=== 1. Testing Direct Recipe Name Searches ===")
direct_searches = [
    "Chicken Biryani",
    "Paneer Butter Masala",
    "Masala Dosa",
    "Butter Chicken",
    "Fried Rice",
    "Pad Thai",
    "Chicken Teriyaki"
]

search_results = {}
for term in direct_searches:
    data = http_get("recipes", {"query": term, "size": 5})
    page_content = data.get("data", {}).get("content", [])
    total_found = data.get("data", {}).get("totalElements", 0)
    top_titles = [r.get("title") for r in page_content[:3]]
    search_results[term] = {
        "total_elements": total_found,
        "top_matches": top_titles
    }
    print(f"  Query: '{term}' -> Found: {total_found} recipes. Top: {top_titles}")

results["direct_searches"] = search_results

print("\n=== 2. Testing Ingredient-Based Recommendations ===")
ingredient_tests = [
    {"name": "Chicken + Tomato + Onion + Garlic", "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"]},
    {"name": "Paneer + Tomato + Onion + Butter", "ingredients": ["Paneer", "Tomato", "Onion", "Butter"]},
    {"name": "Rice + Chicken + Onion + Spices", "ingredients": ["Rice", "Chicken", "Onion", "Garam Masala"]},
    {"name": "Potato + Cumin + Chilli", "ingredients": ["Potato", "Cumin Seeds", "Green Chilli"]}
]

recom_results = {}
for it in ingredient_tests:
    payload = {
        "ingredients": it["ingredients"],
        "usePantry": False
    }
    resp = http_post("recommendations/by-ingredients", payload)
    data = resp.get("data", [])
    top_recoms = []
    for r in data[:3]:
        top_recoms.append({
            "title": r.get("recipe", {}).get("title"),
            "cuisine": r.get("recipe", {}).get("cuisine"),
            "matchScore": r.get("matchScore"),
            "matchedCount": len(r.get("matchedIngredients", [])),
            "missingCount": len(r.get("missingIngredients", [])),
            "reason": r.get("reason")
        })
    recom_results[it["name"]] = {
        "total_returned": len(data),
        "top_3": top_recoms
    }
    print(f"  Ingredients: {it['ingredients']} -> {len(data)} recommendations.")
    if top_recoms:
        print(f"    Top: {top_recoms[0]['title']} ({top_recoms[0]['cuisine']}) - Score: {top_recoms[0]['matchScore']}")

results["ingredient_recommendations"] = recom_results

print("\n=== 3. Testing Edge Cases & Preferences ===")
edge_cases = [
    {
        "case": "1. Single ingredient",
        "payload": {"ingredients": ["Potato"], "usePantry": False}
    },
    {
        "case": "2. Aliased ingredient (dhania, scallion, hari mirch, dahi)",
        "payload": {"ingredients": ["dhania", "scallion", "hari mirch", "dahi"], "usePantry": False}
    },
    {
        "case": "3. Plural ingredient variants (onions, tomatoes, potatoes)",
        "payload": {"ingredients": ["onions", "tomatoes", "potatoes"], "usePantry": False}
    },
    {
        "case": "4. Dietary restriction: VEGETARIAN",
        "payload": {"ingredients": ["Chicken", "Paneer", "Spinach"], "dietaryType": "VEGETARIAN", "usePantry": False}
    },
    {
        "case": "5. Cuisine filter: Indian",
        "payload": {"ingredients": ["Chicken", "Rice", "Onion"], "cuisine": "Indian", "usePantry": False}
    },
    {
        "case": "6. Cuisine filter: Chinese",
        "payload": {"ingredients": ["Chicken", "Rice", "Soy Sauce"], "cuisine": "Chinese", "usePantry": False}
    },
    {
        "case": "7. Cooking time filter (<= 25 mins)",
        "payload": {"ingredients": ["Egg", "Onion", "Tomato"], "maxCookingTime": 25, "usePantry": False}
    },
    {
        "case": "8. User pantry inclusion (User 1 - Rahul)",
        "payload": {"userId": 1, "usePantry": True}
    }
]

edge_results = {}
for ec in edge_cases:
    resp = http_post("recommendations/by-ingredients", ec["payload"])
    data = resp.get("data", [])
    top = data[0] if data else {}
    rec_info = {
        "count": len(data),
        "top_title": top.get("recipe", {}).get("title"),
        "top_cuisine": top.get("recipe", {}).get("cuisine"),
        "top_diet": top.get("recipe", {}).get("dietaryType"),
        "top_time": top.get("recipe", {}).get("totalTimeMinutes"),
        "top_score": top.get("matchScore")
    }
    edge_results[ec["case"]] = rec_info
    print(f"  {ec['case']} -> Count: {len(data)}, Top: {rec_info['top_title']} ({rec_info['top_cuisine']})")

results["edge_cases"] = edge_results

# Save verification results
audit_dir = os.path.join(os.path.dirname(__file__), "..", "data", "audit")
os.makedirs(audit_dir, exist_ok=True)
with open(os.path.join(audit_dir, "query_test_results.json"), "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)
print("\nSaved all verification results to data/audit/query_test_results.json")
