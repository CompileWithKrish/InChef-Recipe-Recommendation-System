# InChef — REST & SOAP API Documentation

All REST API endpoints are versioned under the base prefix `/api/v1`.  
SOAP reporting services are hosted at `/ws`.

---

## 1. Authentication & Security Endpoints (`/api/v1/auth`)

### 1.1 User Registration
- **Method:** `POST /api/v1/auth/register`
- **Authentication:** Public
- **Request Body:**
  ```json
  {
    "email": "chef@example.com",
    "name": "Priya Sharma",
    "password": "SecurePassword123"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "user": {
        "id": 1,
        "email": "chef@example.com",
        "name": "Priya Sharma",
        "avatarUrl": null
      }
    }
  }
  ```

### 1.2 User Login
- **Method:** `POST /api/v1/auth/login`
- **Authentication:** Public
- **Request Body:**
  ```json
  {
    "email": "chef@example.com",
    "password": "SecurePassword123"
  }
  ```
- **Success Response (200 OK):** Returns JWT token and user profile; records successful event in `login_history`.

### 1.3 Google OAuth Sign-In
- **Method:** `POST /api/v1/auth/google`
- **Authentication:** Public
- **Request Body:**
  ```json
  {
    "idToken": "google_credential_jwt_from_gsi"
  }
  ```
- **Success Response (200 OK):** Authenticates or provisions user via Google Identity Services and logs `GOOGLE_OAUTH` event in `login_history`.

### 1.4 Get Authenticated User
- **Method:** `GET /api/v1/auth/me`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Success Response (200 OK):** Returns current user information derived from server security context.

---

## 2. Recipe Endpoints (`/api/v1/recipes`)

### 2.1 Direct Recipe Search (Workflow A)
- **Method:** `GET /api/v1/recipes/search?query=Biryani&page=0&size=12`
- **Authentication:** Optional
- **Query Parameters:**
  - `query` (string): Search term matched against title or translated title (case-insensitive).
  - `page` (int, default `0`): Zero-indexed page number.
  - `size` (int, default `12`): Number of recipes per page.
- **Success Response (200 OK):** Paginated `RecipeSummaryResponse` records.

### 2.2 Get Recipe Details
- **Method:** `GET /api/v1/recipes/{id}`
- **Authentication:** Optional (Pass JWT token to receive personalized favorite and pantry match flags).
- **Success Response (200 OK):** Full recipe details including instructions, time, dietary type, and ingredient breakdown.

### 2.3 List Available Cuisines
- **Method:** `GET /api/v1/recipes/cuisines`
- **Authentication:** Public
- **Success Response (200 OK):** Distinct alphabetical list of available cuisine categories.

---

## 3. Recommendation Endpoints (`/api/v1/recommendations`)

### 3.1 Recommend by Ingredients (Workflow B)
- **Method:** `POST /api/v1/recommendations/by-ingredients`
- **Authentication:** Optional (authenticated if `usePantry: true`)
- **Request Body:**
  ```json
  {
    "ingredients": ["Chicken", "Tomato", "Onion", "Garlic"],
    "userId": 1,
    "usePantry": false,
    "dietaryType": "NON_VEGETARIAN",
    "cuisine": "Indian",
    "maxCookingTime": 45,
    "cookingSkillLevel": "INTERMEDIATE",
    "minIngredientMatch": 75.0,
    "limit": 10
  }
  ```
- **Success Response (200 OK):** List of recommendations ranked by the 100-point formula with explainable `scoreBreakdown`.

---

## 4. User Kitchen / Pantry Endpoints (`/api/v1/users/{userId}/pantry`)

All pantry endpoints require valid authentication. Access to another user's pantry is rejected with `403 Forbidden`.

- **`GET /api/v1/users/{userId}/pantry`**: List all items currently stocked in user's kitchen.
- **`POST /api/v1/users/{userId}/pantry`**: Add or update ingredient quantity and unit.
- **`DELETE /api/v1/users/{userId}/pantry/{id}`**: Remove an item from the kitchen.
- **`DELETE /api/v1/users/{userId}/pantry/clear`**: Clear all kitchen ingredients.

---

## 5. User Preferences & Login History

### 5.1 Get / Update Preferences
- **`GET /api/v1/users/{userId}/preferences`**: Retrieve user dietary preferences, spice level, and preferred cuisines array.
- **`PUT /api/v1/users/{userId}/preferences`**: Save updated preferences. Supports array of cuisines (e.g. `["North Indian", "South Indian"]`).

### 5.2 Get Login History
- **`GET /api/v1/users/me/login-history?page=0&size=10`**: Retrieve paginated login history for current authenticated user.
- **`GET /api/v1/users/{userId}/login-history?page=0&size=10`**: Retrieve history verifying that authenticated user matches `userId`.

---

## 6. SOAP Web Service (`/ws/reports`)

WSDL Contract available at: `http://localhost:8080/ws/reports.wsdl`

- **Endpoint:** `/ws`
- **Namespace:** `http://inchef.com/soap/reports`
- **Supported Operations:**
  - `GetRecipePopularityReportRequest` -> `GetRecipePopularityReportResponse`
  - `GetSystemReportRequest` -> `GetSystemReportResponse`

