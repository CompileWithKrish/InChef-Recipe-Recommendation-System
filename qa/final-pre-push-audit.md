# InChef — Final Pre-Push Audit Report

**Audit Timestamp:** October 28, 2026  
**Auditor:** Senior Software Architect & Security Engineer (Antigravity)  
**Target Repository:** InChef — Intelligent Indian Recipe Recommendation System  
**Working Directory:** `<project-root>`  
**Current Git Branch:** `main`  
**Configured Git Remote:** `(None — No remote configured)`  
**Audit Mode:** Read-Only Verification (No code modified, no history rewritten, no commits created, no database altered)  

---

## Executive Summary & Verdict

### Final Verdict: ⚠️ NOT READY TO PUSH

While the application builds cleanly, all **145 automated tests pass**, and the working tree is structurally sound with no Stitch folders present on disk, the repository **cannot be safely pushed to a public or team GitHub repository in its current state**.

A `git push` does not merely push the current working files; **it transmits every commit object in the branch history**. There are four critical pre-push blockers:

1. **Hardcoded Database Credentials & Secret Fallbacks:** Plaintext database passwords and default secret keys exist across 8 legacy pipeline scripts, the comprehensive QA script, and Spring Boot configuration files.
2. **Commit History Exposure of Stitch Assets:** Although `stitch_assets` and `stitch_screens` were deleted in commit `702ed63`, **54 raw Stitch prototype files and screenshots remain permanently stored in the baseline commit `876e99d`**. Pushing will upload these proprietary Stitch mockups to GitHub.
3. **Local Machine Absolute Paths:** 72 instances of local Windows machine paths (`<external-dir> projects\...` and `<workspace>\...`) are committed across pipeline scripts, QA runners, and Markdown reports.
4. **Duplicate Dataset Blobs (24 MB):** Exact byte-for-byte duplicate CSV files exist under both `data/cleaned/` and `data/processed/`, inflating repository size.

---

## 1. Git Repository State

### 1.1 Working Directory and Remote Configuration
- **Working Directory:** `<project-root>`
- **Active Branch:** `main`
- **Remote:** No remote configured (`git remote -v` returned empty).
  > **Note on Upstream Comparison:** Because no GitHub remote is configured, Git cannot calculate ahead/behind commit offsets against a remote tracking branch. The push contents analyzed below represent the exact commits that will be published upon running `git push -u origin main`.

### 1.2 Commit History Proposed for Push
If a remote is attached and pushed now, Git will push **3 commits**:

| Commit Hash | Author Date | Commit Subject | Files Changed |
| :--- | :--- | :--- | :--- |
| `876e99d` | 2026-10-28 | `checkpoint: baseline commit before repository restructuring` | 301 files added |
| `6a039a0` | 2026-10-28 | `refactor: restructure repository into modular feature architecture and GitHub-ready layout` | 370 files changed (+177, -193) |
| `702ed63` | 2026-10-28 | `refactor: integrate Stitch design system and remove standalone Stitch folders` | 55 files changed (+1, -54) |

### 1.3 Working Tree Status
- **Staged Changes:** 0 (None)
- **Unstaged Tracked Changes:** 0 (None)
- **Untracked / Ignored Files:** Working tree is completely clean. All temporary audit scripts were safely removed.

### 1.4 Tracked Files Inventory in HEAD (273 Files Total)
A complete inventory of the 273 files currently tracked in `HEAD` broken down by root directory:

```text
Repository Root (273 tracked files):
├── .github/ (1 file)
│   └── workflows/build.yml
├── data/ (23 files)
│   ├── cleaned/ (2 files: recipes_cleaned.csv [9.25MB], recipe_ingredients_cleaned.csv [2.77MB])
│   ├── dictionary/ (2 files: alias_dictionary.csv, category_dictionary.csv)
│   ├── migrations/ (3 files: V1__baseline_schema.sql, V2__add_recipe_image_metadata.sql, V3__add_login_history.sql)
│   ├── processed/ (2 files: inchef_recipes.csv [9.25MB], inchef_recipe_ingredients.csv [2.77MB])
│   ├── reports/ (3 files: audit_summary_report.md, import_summary_report.md, migration_summary_report.md)
│   ├── samples/ (1 file: sample_recipes.xlsx)
│   └── taxonomies/ (10 files: cuisines, courses, diets, allergens, units, cooking methods, etc.)
├── docs/ (10 files)
│   ├── architecture-overview.md
│   ├── database-schema.md
│   ├── deployment-guide.md
│   ├── development-setup.md
│   ├── image-resolution-strategy.md
│   ├── presentation-guide.md
│   ├── recommendation-algorithm.md
│   ├── security-overview.md
│   ├── testing-strategy.md
│   └── user-guide.md
├── pipeline/ (20 files)
│   ├── Python ETL scripts, schema validators, image scrapers, dataset audit runners
├── qa/ (29 files)
│   ├── QA test scripts, integration test harnesses, verification reports
├── scripts/ (5 files)
│   ├── setup.bat, setup.sh, run.bat, run.sh, import_dataset.bat
├── src/ (177 files)
│   ├── main/java/com/inchef/ (123 Java source files across 8 modular feature packages)
│   ├── main/resources/ (48 files: application.properties, application-mysql.properties, application-h2.properties, static web UI assets)
│   └── test/java/com/inchef/ (6 comprehensive test suites covering 145 test cases)
├── .env.example
├── .gitattributes
├── .gitignore
├── docker-compose.yml
├── LICENSE
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## 2. Stitch Verification

### 2.1 Working Tree Verification
- `stitch_assets/` directory on disk: **DOES NOT EXIST** (Confirmed)
- `stitch_screens/` directory on disk: **DOES NOT EXIST** (Confirmed)
- Any standalone Stitch preview HTML files on disk: **NONE** (Confirmed)

### 2.2 HEAD Commit Tracking
- Tracked files in HEAD matching `stitch`: **0 files** (Except the post-migration audit report `qa/stitch-integration-cleanup-report.md`).
- All active Stitch styles and tokens were integrated directly into `src/main/resources/static/css/stitch-core.css` and `src/main/resources/static/js/stitch-theme.js`.

### 2.3 Critical History Finding: Historical Blobs in Commit `876e99d`
> ⚠️ **CRITICAL WARNING:**  
> In commit `876e99d`, exactly **54 Stitch prototype files and full-page PNG screenshots** were committed to Git.  
> Even though commit `702ed63` deleted these files from the working tree, **Git's commit graph permanently preserves them**. If the user executes `git push`, all 54 Stitch HTML drafts and high-resolution PNG mockups will be uploaded to GitHub and viewable in commit history.

---

## 3. Secrets, Sensitive Data & Absolute Paths

An automated scan of all 273 tracked files in `HEAD` revealed **80 security/privacy findings**:

### 3.1 Hardcoded Plaintext Passwords (8 Occurrences)
The following scripts contain hardcoded root database connection passwords:

| File Path | Line(s) | Category | Remediation |
| :--- | :--- | :--- | :--- |
| `pipeline/analyze_ingredient_usage.py` | 6 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/check_mysql_tables.py` | 7 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/check_pantry_in_ifd.py` | 6 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/describe_tables.py` | 7 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/find_reconcile_targets.py` | 6 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/inspect_db_dataset.py` | 12 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `pipeline/match_favorites.py` | 6 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |
| `qa/run_comprehensive_qa.py` | 718 | MySQL Root Password | Use `os.environ.get("DB_PASSWORD")` |

### 3.2 Configuration Default Fallback Secrets
The Spring Boot configuration files define environment-variable overrides, but supply sensitive local fallbacks:

| File Path | Line | Property | Finding |
| :--- | :--- | :--- | :--- |
| `src/main/resources/application-mysql.properties` | 4 | `spring.datasource.password` | Contains personal database password fallback |
| `src/main/resources/application.properties` | 32 | `inchef.jwt.secret` | Contains static default HMAC secret |
| `src/main/resources/application.properties` | 40 | `inchef.google.client-id` | Contains personal Google OAuth Client ID |
| `src/main/resources/application.properties` | 43 | `inchef.admin.secret` | Contains static default admin secret |

### 3.3 Local Machine Absolute Paths (72 Occurrences)
Absolute paths to the developer's local machine filesystem are present in:
- `pipeline/import_indian_food_dataset.py` (Line 9: `<external-project-dir>\...`)
- `pipeline/audit_datasets.py` (Lines 8-10: `<external-dir> projects\...`, `<workspace>\...`)
- `pipeline/complete_dataset_migration.py`, `recipe_processor.py`, `resolve_recipe_images.py`, `validate_pipeline.py`
- `qa/run_comprehensive_qa.py` (Lines 795–799)
- Historical markdown reports in `data/reports/` and `qa/`

---

## 4. Unnecessary Files, Backups & Duplicate Datasets

### 4.1 Backup and Database Dump Verification
- `data/backup/inchef_db_backup_pre_migration.sql` (58.3 MB): **NOT TRACKED** (Ignored by `.gitignore`)
- `InChef_API&MS.zip` (70+ MB): **NOT TRACKED** (Ignored by `.gitignore`)
- `data/cache/recipe_images_cache.json` (4.7 MB): **NOT TRACKED** (Ignored by `.gitignore`)
- Build outputs (`target/`, `*.class`, `*.jar`): **NOT TRACKED** (Ignored by `.gitignore`)
- IDE / Editor files (`.vscode/`, `.idea/`): **NOT TRACKED** (Ignored by `.gitignore`)

### 4.2 Large Files & Duplicate Datasets Currently Tracked in HEAD
Four large CSV files are currently tracked in Git, totaling **24.04 MB**:

| File Path | Size | MD5 Checksum Match | Finding |
| :--- | :--- | :--- | :--- |
| `data/cleaned/recipes_cleaned.csv` | 9.25 MB | Identical | **Byte-for-byte duplicate** of `data/processed/inchef_recipes.csv` |
| `data/processed/inchef_recipes.csv` | 9.25 MB | Identical | **Byte-for-byte duplicate** of `data/cleaned/recipes_cleaned.csv` |
| `data/cleaned/recipe_ingredients_cleaned.csv` | 2.77 MB | Identical | **Byte-for-byte duplicate** of `data/processed/inchef_recipe_ingredients.csv` |
| `data/processed/inchef_recipe_ingredients.csv` | 2.77 MB | Identical | **Byte-for-byte duplicate** of `data/cleaned/recipe_ingredients_cleaned.csv` |

> **Recommendation:** Track only `data/processed/inchef_recipes.csv` and `data/processed/inchef_recipe_ingredients.csv` (or keep only `data/samples/sample_recipes.xlsx` for repo cloning and store 9MB datasets via GitHub Releases or external storage).

---

## 5. Application Build and Test Verification

### 5.1 Automated Test Execution (`.\mvnw.cmd test`)
- **Execution Profile:** `test` (using H2 in-memory isolated database, zero MySQL dependency).
- **Test Results:**
  - Total Tests Run: **145**
  - Failures: **0**
  - Errors: **0**
  - Skipped: **0**
  - Success Rate: **100%**
  - Execution Time: 21.9 seconds
- **Key Suites Verified:**
  - `UserPreferenceIntegrationTest`: 8 tests passed (single cuisine, multi-cuisine, empty list, deserialization, persistence).
  - `LoginHistoryIntegrationTest`: 12 tests passed (recording, pagination, failed attempts, IP capture).
  - `RecommendationControllerTest` & `RecommendationEngineTest`: 46 tests passed (100-pt formula, allergen guard, threshold filtering).
  - `DirectSearchControllerTest` & `RecipeSearchPerformanceTest`: 34 tests passed (sub-20ms response, pagination, indexing).
  - `AuthControllerTest` & `AdminControllerTest`: 45 tests passed (JWT auth, token refresh, rate limiting, SSRF protection).

### 5.2 Artifact Packaging (`.\mvnw.cmd package -DskipTests`)
- **Build Status:** **BUILD SUCCESS**
- **Packaging Target:** `target/inchef-backend-1.0.0.jar`
- **Embedded Web Server:** Tomcat 10.1.30 initialized on port 8080.

### 5.3 Frontend Static Assets Integrity
- **Total Static Files:** 44 files in `src/main/resources/static/`
- **Internal Asset References:** 66 CSS, JS, icon, and font linkages across all 8 HTML templates.
- **Broken Links:** **0** (All paths resolve to valid files).

### 5.4 Git Diff Whitespace & Conflict Check (`git diff --check`)
- Result: Clean. Zero merge markers, zero whitespace errors.

---

## 6. GitHub Readiness Configuration

| File | Status | Description |
| :--- | :--- | :--- |
| `.gitignore` | ✅ Excellent | Excludes `.env`, `target/`, `*.sql`, `data/backup/`, `data/cache/`, `.idea/`, `.vscode/`, `scratch/`. |
| `.env.example` | ✅ Clean | Comprehensive template with dummy values for all database, JWT, OAuth, and admin keys. |
| `README.md` | ✅ Complete | Professional, production-grade documentation with architecture diagrams, setup commands, and feature specs. |
| `LICENSE` | ✅ Present | MIT License. |
| `docker-compose.yml` | ✅ Ready | Multi-container setup for MySQL 8.0 with automatic migration mounting and healthcheck. |
| `.github/workflows/build.yml` | ✅ Ready | GitHub Actions CI workflow running JDK 17, H2 profile automated tests, and artifact packaging. |
| Maven Wrapper (`mvnw`, `mvnw.cmd`) | ✅ Present | Allows CI and developers to build without requiring a pre-installed Maven binary. |

---

## 7. Actionable Pre-Push Remediation Guide

To make the repository 100% production-ready and safe for GitHub, follow these 3 safe steps before pushing:

### Step 1: Sanitize Sensitive Defaults and Local Paths
1. In `src/main/resources/application-mysql.properties`, change:
   ```properties
   spring.datasource.password=${DB_PASSWORD:}
   ```
2. In `pipeline/*.py` and `qa/run_comprehensive_qa.py`, replace plaintext default passwords with `os.environ.get("DB_PASSWORD", "")`.
3. In `pipeline/*.py`, replace hardcoded `<external-dir>...` with relative paths (`os.path.join(os.path.dirname(__file__), "../data/...")`).

### Step 2: Remove Redundant Duplicate Datasets
Remove `data/cleaned/` from Git tracking (leaving `data/processed/` as the single canonical dataset location).

### Step 3: Clean Git History Before Initial Push
Because `origin` has not yet been pushed to, **squashing the 3 local commits into a single clean initial commit** is the safest, cleanest industry practice:
```powershell
# Create a fresh orphan branch with the clean tree
git checkout --orphan github-release

# Add all files
git add -A

# Commit the clean, unified codebase
git commit -m "feat: initial release of InChef recipe recommendation platform"

# Rename to main and push
git branch -M github-release main
git remote add origin https://github.com/<your-username>/inchef.git
git push -u origin main
```
*Benefits of this method:*
- Permanently purges all 54 deleted Stitch prototype mockups from Git history.
- Leaves zero trace of historical password commits.
- Results in a tiny, fast `git clone` without bloated historical Git objects.

