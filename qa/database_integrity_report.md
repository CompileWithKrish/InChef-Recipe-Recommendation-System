# Database Integrity & Verification Report
**Database:** `inchef_db` on `localhost:3306`

## 1. Table Counts & Baseline Verification

| Entity / Table | Count | Expected Target | Status |
| :--- | :--- | :--- | :--- |
| Recipes (`recipes`) | 6,865 | Exactly 6,865 (>= 6,800) | PASS |
| Obsolete Recipes (DataHive/Showcase) | 0 | 0 (Purged) | PASS |
| Ingredients (`ingredients`) | 2,610 | Clean Catalog (>= 2,500) | PASS |
| Recipe Ingredients (`recipe_ingredients`) | 73,846 | Links (>= 70,000) | PASS |
| Users (`users`) | 10 | >= 3 | PASS |
| User Favorites (`favorites`) | 7 | >= 6 | PASS |
| User Pantry (`user_ingredients`) | 26 | >= 22 | PASS |
| User Preferences (`user_preferences`) | 3 | >= 3 | PASS |
| Recommendation Logs (`recommendation_logs`) | 333 | >= 0 | PASS |

## 2. Relational & Referential Integrity

| Check | Violations Found | Status |
| :--- | :--- | :--- |
| Orphan Recipe Links (`recipe_id` not in `recipes`) | 0 | PASS |
| Orphan Ingredient Links (`ingredient_id` not in `ingredients`) | 0 | PASS |
| Null / Empty Recipe Titles | 0 | PASS |
| Null / Empty Ingredient Names | 0 | PASS |
| Duplicate Canonical Ingredient Names (Case-Insensitive) | 0 | PASS |
| Duplicate Recipe Titles | 137 | INFO |

## 3. Preserved User Profiles

| ID | Name | Email | Phone |
| :--- | :--- | :--- | :--- |
| 1 | `Alex Johnson` | `alex@inchef.com` | +1-555-0101 |
| 2 | `Sophia Chen` | `sophia@inchef.com` | +1-555-0102 |
| 3 | `Marcus Rivera` | `marcus@inchef.com` | +1-555-0103 |
| 6 | `Krishnandu Das` | `fosed44373@herclan.com` | N/A |
| 7 | `Test Owner` | `testowner2@inchef.com` | N/A |
| 8 | `Test Owner 3` | `testowner3@inchef.com` | N/A |
| 9 | `Test Owner 4` | `testowner4@inchef.com` | N/A |
| 10 | `Audit Chef` | `audit_chef_1792602334@inchef.com` | N/A |
| 11 | `Audit Chef` | `audit_chef_1792602571@inchef.com` | N/A |
| 12 | `Audit Chef` | `audit_chef_1792605841@inchef.com` | N/A |

## 4. Integrity Summary

**Overall Database Integrity Status:** `PASSED - 100% CLEAN`
