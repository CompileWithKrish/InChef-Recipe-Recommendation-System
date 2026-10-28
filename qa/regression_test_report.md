# InChef — Automated Regression Test Suite Report

**Date:** October 28, 2026  
**Environment:** Java 17 LTS | Spring Boot 3.3.4 | Apache Maven 3.9.6 | In-Memory H2 (`jdbc:h2:mem:inchef_db;MODE=MySQL`)  
**Execution Command:** `.\mvnw.cmd test`  
**Result:** **145 PASSED, 0 FAILURES, 0 ERRORS, 0 SKIPPED** (100% Pass Rate)

---

## 1. Test Execution Breakdown by Feature Module

| Feature Package | Test Class | Tests Run | Failures | Errors | Status |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **`com.inchef`** | `InChefApplicationTests` | 1 | 0 | 0 | **PASS** |
| **`com.inchef.auth`** | `AuthServiceTest` | 13 | 0 | 0 | **PASS** |
| **`com.inchef.auth`** | `GoogleAuthTest` | 11 | 0 | 0 | **PASS** |
| **`com.inchef.security`** | `InChefSecurityAndFiltersTest` | 12 | 0 | 0 | **PASS** |
| **`com.inchef.security`** | `InChefRequirementsTest` | 22 | 0 | 0 | **PASS** |
| **`com.inchef.preference`**| `UserPreferencesAndLoginHistoryIntegrationTest` | 13 | 0 | 0 | **PASS** |
| **`com.inchef.favorite`** | `FavoriteServiceTest` | 7 | 0 | 0 | **PASS** |
| **`com.inchef.favorite`** | `FavoriteControllerIntegrationTest` | 5 | 0 | 0 | **PASS** |
| **`com.inchef.pantry`** | `UserKitchenControllerIntegrationTest` | 5 | 0 | 0 | **PASS** |
| **`com.inchef.recipe`** | `RecipeServiceTest` | 8 | 0 | 0 | **PASS** |
| **`com.inchef.recommendation`** | `RecommendationEngineTest` | 10 | 0 | 0 | **PASS** |
| **`com.inchef.recommendation`** | `RecommendationServiceIntegrationTest` | 9 | 0 | 0 | **PASS** |
| **`com.inchef.recipeimage`** | `RecipeImageControllerTest` | 9 | 0 | 0 | **PASS** |
| **`com.inchef.recipeimage`** | `RecipeHtmlMetadataExtractorTest` | 5 | 0 | 0 | **PASS** |
| **`com.inchef.recipeimage`** | `RecipeImageResolutionServiceTest` | 6 | 0 | 0 | **PASS** |
| **`com.inchef.user`** | `UserServiceTest` | 2 | 0 | 0 | **PASS** |
| **`com.inchef.common`** | `ImageQualityProberTest` | 4 | 0 | 0 | **PASS** |
| **`com.inchef.common`** | `SsrfValidatorTest` | 3 | 0 | 0 | **PASS** |
| **TOTAL** | **18 Test Classes** | **145** | **0** | **0** | **100% PASS** |

---

## 2. Key Verifications Confirmed

1. **User Preferences Array Deserialization:**
   - Single cuisine selection produces `["North Indian"]`.
   - Multiple cuisine selections produce `["North Indian", "South Indian"]`.
   - Empty cuisine selections produce empty array `[]`.
   - Malformed/string payloads are validated and rejected with HTTP 400.

2. **Login History Feature:**
   - Database table `login_history` records `user_id`, `login_at` timestamp, `authentication_method` (`PASSWORD` vs `GOOGLE_OAUTH`), and `status` (`SUCCESS`).
   - Secure authenticated endpoint `GET /api/v1/users/me/login-history` and `/{userId}/login-history` verify user authorization.
   - Cross-user data access is strictly forbidden with HTTP 403.

3. **Workflow A (Direct Dish Search):**
   - Exact and partial match queries execute cleanly against indexed title and translated title fields.
   - Bypasses ingredient tokenization and scoring.

4. **Workflow B (Ingredient-Based Recommendation):**
   - 75% coverage threshold strictly enforced.
   - Non-bypassable allergy constraint enforced.
   - 100-point multi-factor ranking formula validated.

5. **Security & SSRF Protection:**
   - Loopback and cloud metadata endpoints (`127.0.0.1`, `169.254.169.254`, `10.0.0.0/8`) strictly blocked by `SsrfValidator`.

