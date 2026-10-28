# Ingredient Data Cleansing Dry-Run Report
**Total Existing Canonical Ingredients:** 6,288
**Total Proposed Transformations:** 533
- **Merges into Existing Canonical Ingredients:** 284
- **In-place Clean Renames:** 249
- **Affected Recipe Ingredient Links:** 3,142
- **Affected User Pantry Items:** 0
- **Ambiguous / Protected Preserved Untouched:** 2,140

## 1. Sample Merges (First 30)
| Source ID | Source Name | Target ID | Target Canonical Name | Recipe Links | Pantry Links |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 39 | `0 25 Cup Gram Flour` | 1608 | `Gram Flour` | 1 | 0 |
| 40 | `0 25 Cup Homemade Tomato Puree` | 1776 | `Homemade Tomato Puree` | 1 | 0 |
| 41 | `0 25 Cup Mint Leave` | 5032 | `Mint Leave` | 1 | 0 |
| 42 | `0 5 Inch Cinnamon Stick` | 5043 | `Cinnamon Stick` | 1 | 0 |
| 43 | `0 Clove` | 1227 | `Clove` | 3 | 0 |
| 53 | `1 4 Cup Sugar` | 3715 | `Sugar` | 1 | 0 |
| 56 | `1 Cottage Cheese` | 5282 | `Cottage Cheese` | 1 | 0 |
| 57 | `1 Teaspoon Asafoetida` | 802 | `Asafoetida` | 1 | 0 |
| 58 | `1 Teaspoon Red Chilli Powder` | 3305 | `Red Chilli Powder` | 2 | 0 |
| 59 | `1 Tsp Oil` | 5031 | `Oil` | 1 | 0 |
| 60 | `1 Tsp Turmeric Powder` | 5023 | `Turmeric Powder` | 2 | 0 |
| 413 | `1-2 Cup Hot Water` | 5148 | `Hot Water` | 0 | 0 |
| 420 | `1-2 Tablespoon Chocolate Chip` | 5142 | `Chocolate Chip` | 0 | 0 |
| 421 | `1-2 Tablespoon Ghee` | 1566 | `Ghee` | 0 | 0 |
| 423 | `1-2 Tablespoon Sunflower Oil` | 3728 | `Sunflower Oil` | 0 | 0 |
| 426 | `1-2 Teaspoon Red Chilli Powder` | 3305 | `Red Chilli Powder` | 0 | 0 |
| 427 | `1-2 Teaspoon Red Wine` | 5492 | `Red Wine` | 0 | 0 |
| 428 | `1-2 Teaspoon Sugar` | 3715 | `Sugar` | 0 | 0 |
| 438 | `10-12 Cup Water` | 3915 | `Water` | 0 | 0 |
| 439 | `10-12 Curry Leave` | 5028 | `Curry Leave` | 0 | 0 |
| 441 | `10-15 Curry Leave` | 5028 | `Curry Leave` | 0 | 0 |
| 442 | `10-15 Mint Leave` | 5032 | `Mint Leave` | 0 | 0 |
| 443 | `10-15 Whole Black Pepper` | 3951 | `Whole Black Pepper` | 0 | 0 |
| 444 | `10-15 Whole Black Peppercorn` | 3953 | `Whole Black Peppercorn` | 0 | 0 |
| 445 | `12-14 Whole Black Peppercorn` | 3953 | `Whole Black Peppercorn` | 0 | 0 |
| 447 | `15-20 Cashew Nut` | 1058 | `Cashew Nut` | 0 | 0 |
| 448 | `15-20 Raisin` | 3259 | `Raisin` | 0 | 0 |
| 462 | `2 Milk` | 31 | `Milk` | 3 | 0 |
| 463 | `2 Tablespoon Mint Leave` | 5032 | `Mint Leave` | 1 | 0 |
| 464 | `2 Tablespoon Sunflower Oil` | 3728 | `Sunflower Oil` | 1 | 0 |

## 2. Sample In-Place Clean Renames (First 30)
| ID | Original Name | Cleaned Name | Recipe Links | Pantry Links |
| :--- | :--- | :--- | :--- | :--- |
| 38 | `0 25 Cup Curd` | `Curd` | 1 | 0 |
| 55 | `1 Clove Laung` | `Laung` | 8 | 0 |
| 414 | `1-2 Dry Red Chilli` | `Dry Red Chilli` | 0 | 0 |
| 415 | `1-2 Dry Red Chilly` | `Dry Red Chilly` | 0 | 0 |
| 416 | `1-2 Green Chilly - Chopped` | `Green Chilly - Chopped` | 0 | 0 |
| 417 | `1-2 Green Chilly - Finely Chopped` | `Green Chilly - Finely Chopped` | 0 | 0 |
| 418 | `1-2 Green Chilly - Slit` | `Green Chilly - Slit` | 0 | 0 |
| 419 | `1-2 Green Chilly - Slit Lengthwise` | `Green Chilly - Slit Lengthwise` | 0 | 0 |
| 422 | `1-2 Tablespoon Pistachio - Coarsely Powdered` | `Pistachio - Coarsely Powdered` | 0 | 0 |
| 424 | `1-2 Teaspoon Malt Vinegar` | `Malt Vinegar` | 0 | 0 |
| 425 | `1-2 Teaspoon Mango Pickle - Optional` | `Mango Pickle - Optional` | 0 | 0 |
| 435 | `10 Clove Laung` | `Laung` | 3 | 0 |
| 436 | `10 Kilogram` | `Kilogram` | 1 | 0 |
| 440 | `10-12 Curry Leave - Torn` | `Curry Leave - Torn` | 0 | 0 |
| 446 | `13-15 Betel Leave Paan - Vetrilai` | `Betel Leave Paan - Vetrilai` | 0 | 0 |
| 449 | `15-20 Soy Chunk Nugget - Soaked In Warm Water For 20 Minute` | `Soy Chunk Nugget - Soaked In Warm Water For 20 Minute` | 0 | 0 |
| 454 | `2 Clove Laung` | `Laung` | 172 | 0 |
| 455 | `2 Clove Laung -` | `Laung` | 0 | 0 |
| 456 | `2 Clove Laung - 2 Clove` | `Laung - 2 Clove` | 0 | 0 |
| 457 | `2 Clove Laung - Finely Chopped` | `Laung - Finely Chopped` | 0 | 0 |
| 458 | `2 Clove Laung - Garlic` | `Laung - Garlic` | 0 | 0 |
| 459 | `2 Clove Laung - Pounded` | `Laung - Pounded` | 0 | 0 |
| 460 | `2 Greek Yogurt` | `Greek Yogurt` | 1 | 0 |
| 461 | `2 Large Tablespoon Sesame` | `Large Tablespoon Sesame` | 1 | 0 |
| 466 | `2 To 3 Clove Laung` | `To 3 Clove Laung` | 4 | 0 |
| 522 | `2-2 Green Chilly - Crushed` | `Green Chilly - Crushed` | 0 | 0 |
| 523 | `2-2 Jackfruit Ripe Kathal - Ripe` | `Jackfruit Ripe Kathal - Ripe` | 0 | 0 |
| 527 | `2-3 Cardamom Elaichi Pod Seed` | `Cardamom Elaichi Pod Seed` | 0 | 0 |
| 528 | `2-3 Clove Laung` | `Laung` | 0 | 0 |
| 529 | `2-3 Cup Almond Milk Badam Milk` | `Almond Milk Badam Milk` | 0 | 0 |

## 3. Sample Ambiguous / Protected Preserved Records (First 15)
| ID | Name | Preserved Reason |
| :--- | :--- | :--- |
| 36 | `-3 Green Chilly - Sliced` | Ambiguous or non-alphabetical result - preserved untouched |
| 37 | `0 25` | Ambiguous or non-alphabetical result - preserved untouched |
| 44 | `0 Fat Greek Yogurt` | Protected multi-word / culinary identity ingredient |
| 45 | `1` | Ambiguous or non-alphabetical result - preserved untouched |
| 46 | `1 - 2 -` | Ambiguous or non-alphabetical result - preserved untouched |
| 47 | `1 -1 2 Turmeric Powder` | Ambiguous or non-alphabetical result - preserved untouched |
| 48 | `1 1 2 - 2 Cup Jaggery - Grated` | Ambiguous or non-alphabetical result - preserved untouched |
| 49 | `1 1 2 - 2 Tablespoon Sugar - Adjustable` | Ambiguous or non-alphabetical result - preserved untouched |
| 50 | `1 2` | Ambiguous or non-alphabetical result - preserved untouched |
| 51 | `1 4` | Ambiguous or non-alphabetical result - preserved untouched |
| 52 | `1 4 Cabbage` | Ambiguous or non-alphabetical result - preserved untouched |
| 54 | `1 4 Hung Curd` | Ambiguous or non-alphabetical result - preserved untouched |
| 61 | `1- 1 1 4 Cup Water` | Ambiguous or non-alphabetical result - preserved untouched |
| 62 | `1-1 1 2 Cup Mango Ripe - Raw Unripe` | Ambiguous or non-alphabetical result - preserved untouched |
| 63 | `1-1 1 2 Cup Rice` | Ambiguous or non-alphabetical result - preserved untouched |