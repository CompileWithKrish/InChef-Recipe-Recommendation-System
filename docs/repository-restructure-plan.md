# InChef Repository Restructuring & Migration Plan

**Date:** October 28, 2026  
**System:** InChef — Intelligent Indian Recipe Recommendation System  
**Baseline Git Checkpoint:** Initialized & committed on `main` branch.

---

## 1. Inventory & Classification Matrix

Each existing file and directory has been evaluated against runtime dependency, build configuration, and Git readiness:

| Current Path | Classification | Target Path / Action | Rationale |
| :--- | :--- | :--- | :--- |
| `INCHEF_BACKEND_DATABASE.md` | **MOVE** | `docs/database-schema.md` | Formal documentation of MySQL 8 schema, entities, and constraints. |
| `INCHEF_RECOMMENDATION_LOGIC.md` | **MOVE** | `docs/recommendation-engine.md` | Full algorithmic specification of multi-factor ranking. |
| `inchef_retrieval_audit_report.md` | **MOVE** | `docs/image-retrieval-engine.md` | Technical documentation of Strategy A/B/C/D image pipeline. |
| `.env.example` | **KEEP** | `.env.example` | Template for environment variables (sanitized of real credentials). |
| `docker-compose.yml` | **KEEP** | `docker-compose.yml` | Standardized MySQL 8 container configuration for local development. |
| `mvnw.cmd` | **KEEP** | `mvnw.cmd` | Windows Maven wrapper delegating to bundled Maven. |
| `mvnw` | **KEEP** | `mvnw` | POSIX shell script wrapper for Linux/macOS and CI/CD pipelines. |
| `pom.xml` | **KEEP** | `pom.xml` | Maven build definition and project dependencies. |
| `.gitignore` | **KEEP** | `.gitignore` | Comprehensive ignore rules for Java, Maven, IDEs, and local dumps. |
| `.gitattributes` | **KEEP** | `.gitattributes` | Text normalization and line-ending rules for cross-platform Git. |
| `.tools/` | **IGNORE** | `.tools/` | Local bundled Maven distribution (ignored from Git tracking). |
| `target/` | **IGNORE** | `target/` | Compiled classes, reports, and build artifacts. |
| `.vscode/` | **IGNORE** | `.vscode/` | Local editor workspace settings. |
| `components/` | **REMOVE** | *(Deleted)* | Empty scaffold directory. |
| `data/backup/*.sql` | **IGNORE** | `data/backup/*.sql` | 58MB raw database dump; must not be tracked in Git. |
| `data/LICENSES.md` | **KEEP** | `data/LICENSES.md` | Attribution and licensing for datasets. |
| `data/cleaned/` | **KEEP** | `data/cleaned/` | Intermediate cleaned CSV artifacts. |
| `data/processed/` | **KEEP** | `data/processed/` | Canonical CSV tables ready for MySQL import. |
| `data/rejected/` | **KEEP** | `data/rejected/` | Audit records of rejected entries. |
| `data/audit/` | **KEEP** | `data/audit/` | Dataset profiling and statistical summaries. |
| `data/reports/` | **KEEP** | `data/reports/` | QA reconciliation reports. |
| `data/migrations/*.sql` | **KEEP** | `data/migrations/*.sql` | Versioned SQL schema migration scripts. |
| `data/samples/sample_recipes.xlsx` | **CREATE** | `data/samples/sample_recipes.xlsx` | Compact, safe Excel sample for testing import without private paths. |
| `data/README.md` | **CREATE** | `data/README.md` | Documentation on dataset format, schema, and sample usage. |
| `pipeline/` | **MERGE / MOVE** | `scripts/` | Consolidated into standardized operational CLI scripts. |
| `qa/` | **KEEP** | `qa/` | Comprehensive test reports, live verification tools, and audit runners. |
| `scratch/` | **IGNORE** | `scratch/` | Local test scripts and diagnostic outputs. |
| `stitch_screens/` | **INTEGRATED & REMOVED** | `docs/design-system.*`, `src/main/resources/static/` | Design system tokens integrated into docs; UI screens integrated into static templates. |
| `stitch_assets/` | **INTEGRATED & REMOVED** | `src/main/resources/static/images/` | Canonical brand & food image assets integrated with semantic names; raw mocks removed. |

---

## 2. Backend Java Package Restructuring Plan

To transition from generic technical packaging (`controller`, `service`, `entity`, `repository`, `dto`) to business feature modules:

| Feature Module | Classes Included |
| :--- | :--- |
| **`com.inchef.common`** | `ApiResponse`, `ErrorResponse`, `MeasurementUnit`, `SsrfValidator`, `ImageQualityProber`, `LoggingAspect`. |
| **`com.inchef.config`** | `SecurityConfig`, `CorsConfig`, `WebServiceConfig`. |
| **`com.inchef.security`** | `JwtTokenProvider`, `JwtAuthenticationFilter`, `UserPrincipal`, `SecurityUtils`. |
| **`com.inchef.exception`** | `GlobalExceptionHandler`, `BadRequestException`, `ConflictException`, `ResourceNotFoundException`, `UnauthorizedException`. |
| **`com.inchef.auth`** | `AuthController`, `AuthService`, `GoogleAuthService`, `LoginRequest`, `RegisterRequest`, `GoogleLoginRequest`, `AuthResponse`. |
| **`com.inchef.user`** | `User`, `UserRepository`, `UserService`, `UserController`, `UserCreateRequest`, `UserUpdateRequest`, `UserResponse`. |
| **`com.inchef.preference`**| `UserPreference`, `UserPreferenceRepository`, `UserPreferenceService`, `UserPreferenceController`, `UserPreferenceRequest`, `UserPreferenceResponse`, `DietaryType`, `SpiceLevel`, `CookingSkillLevel`. |
| **`com.inchef.loginhistory`** | `LoginHistory`, `LoginHistoryRepository`, `LoginHistoryService`, `LoginHistoryResponse`. |
| **`com.inchef.ingredient`**| `Ingredient`, `IngredientRepository`, `IngredientService`, `IngredientController`, `IngredientCreateRequest`, `IngredientResponse`. |
| **`com.inchef.pantry`** | `UserIngredient`, `UserIngredientRepository`, `UserIngredientService`, `UserKitchenController`, `UserIngredientRequest`, `UserIngredientResponse`. |
| **`com.inchef.favorite`** | `Favorite`, `FavoriteRepository`, `FavoriteService`, `FavoriteController`, `FavoriteResponse`. |
| **`com.inchef.recipe`** | `Recipe`, `RecipeIngredient`, `RecipeRepository`, `RecipeIngredientRepository`, `RecipeService`, `RecipeController`, `RecipeCreateRequest`, `RecipeIngredientRequest`, `RecipeSummaryResponse`, `RecipeDetailResponse`, `RecipeIngredientResponse`, `DifficultyLevel`, `MealType`, `CookingProgressController`, `CookingProgressService`, `CookingProgressRequest`, `CookingProgressResponse`, `UserRecipeProgress`, `UserRecipeProgressRepository`. |
| **`com.inchef.recommendation`** | `Recommendation`, `RecommendationRepository`, `RecommendationEngine`, `RecommendationService`, `RecommendationController`, `DishRecommendationRequest`, `IngredientRecommendationRequest`, `RecommendationResponse`, `ScoreBreakdown`, `RecommendationMode`. |
| **`com.inchef.recipeimage`** | `RecipeImageController`, `RecipeImageResolutionService`, `RecipeHtmlMetadataExtractor`, `ImageSearchProvider`, `GoogleCustomSearchImageProvider`, `RecipeImageResponse`, `RecipeImageAuditReport`, `RecipeImageBatchStatus`, `ImageSourceType`, `ImageStatus`. |
| **`com.inchef.soap`** | `RecipeReportEndpoint`, `RecipeReportService`, `CuisineStat`, `RecipePopularityStat`, `GetRecipePopularityReportRequest`, `GetRecipePopularityReportResponse`, `GetSystemReportRequest`, `GetSystemReportResponse`. |
| **`com.inchef.dataset`** | `DataInitializer`. |

---

## 3. Frontend Static Assets Plan

All static resources remain in `src/main/resources/static/` to preserve Spring Boot embedded hosting:

- Primary Pages: `index.html`, `discover.html`, `recommendations.html`, `recipe_details.html`, `my_kitchen.html`, `favorites.html`, `profile.html`, `auth.html`.
- Route Aliases: `login.html` & `signup.html` redirecting to `auth.html` with appropriate `?mode=login` / `?mode=signup` parameters.
- Desktop Redirectors: Retained for backwards compatibility (`home_desktop.html`, `profile_desktop.html`, etc.).
- Modular Styles: Retain `css/global.css` and `css/splash.css`, establish `css/components.css` and `css/responsive.css` references.
- Modular Scripts: Retain `js/api.js`, `js/header.js`, `js/app.js`, `js/splash.js`.
- Image Assets: Retain canonical logos, icons, and `neutral_recipe_placeholder.svg`.

---

## 4. Scripts & QA Automation Plan

Consolidate root/pipeline scripts into a clean `scripts/` directory:
- `scripts/import_dataset.py` — Resumable CLI importer for `IndianFoodDatasetXLS.xlsx` using `INCHEF_DATASET_PATH`.
- `scripts/audit_dataset.py` — Dataset validator and integrity checker.
- `scripts/audit_database.py` — Database table, index, and relational integrity prober.
- `scripts/audit_recipe_images.py` — Image resolution quality and reachability checker.
- `scripts/README.md` — Usage guide and execution prerequisites.

Keep verification and reconciliation suites in `qa/`:
- `qa/dataset_database_reconciliation_report.md`
- `qa/recipe_image_quality_report.md`
- `qa/regression_test_report.md`
- `qa/regression_verifier.py`

---

## 5. Execution Strategy & Verification Gates

1. **Step 1:** Establish documentation (`docs/`) and scripts (`scripts/`).
2. **Step 2:** Modularize frontend with route aliases (`login.html`, `signup.html`).
3. **Step 3:** Restructure Java packages by business feature with automated import and package realignment.
4. **Step 4:** Compile via `.\mvnw.cmd test-compile` and run complete test suite `.\mvnw.cmd test`.
5. **Step 5:** Create GitHub Actions CI workflow (`.github/workflows/build.yml`).
6. **Step 6:** Update `README.md` with full architectural guide, diagrams, setup instructions, and verify localhost server.

