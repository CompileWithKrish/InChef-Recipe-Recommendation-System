# InChef

## An Intelligent Recipe Recommendation System

**InChef** is an intelligent recipe recommendation system that recommends suitable recipes based on the **ingredients available with the user or the dish they want to prepare**.

The project is being developed as part of the **API & Microservices** course.

---

## 🚧 Project Status

**In Development**

---

## 🎯 Core Concept

InChef provides two main ways to discover recipes:

### 🥕 Ingredient-Based Recommendation

Users enter the ingredients they already have, and InChef recommends recipes that can be prepared using those ingredients.

```text
Available Ingredients
        ↓
Ingredient Matching
        ↓
Recipe Scoring
        ↓
Recommended Recipes
```
### 🍝 Dish-Based Recommendation

Users enter the dish they want to prepare, and InChef identifies suitable recipes along with available and missing ingredients.
```text
Desired Dish
     ↓
Recipe Search
     ↓
Ingredient Analysis
     ↓
Available / Missing Ingredients
     ↓
Recommended Recipes
```
---
## 🏗️ Planned Architecture
```text
                    ┌──────────────┐
                    │     User     │
                    └──────┬───────┘
                           │
                 ┌─────────┴─────────┐
                 │                   │
                 ▼                   ▼
        Available Ingredients    Desired Dish
                 │                   │
                 └─────────┬─────────┘
                           ▼
                ┌─────────────────────┐
                │ InChef Recommendation│
                │       Engine         │
                └──────────┬──────────┘
                           │
                           ▼
                    Recipe Matching
                           │
                           ▼
                  Ranked Recommendations
                           │
                           ▼
                    REST API Response
```
---
## 👨‍💻 Development Team

Project: InChef

Title: An Intelligent Recipe Recommendation System

Team: Team of InChef
