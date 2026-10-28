# Dataset Licenses and Provenance Notice

This document details the licensing, attribution, and terms of use for the datasets utilized in the **InChef** recipe ingestion pipeline.

---

## 1. Indian Food Dataset (`IndianFoodDatasetXLS.xlsx`)
- **Primary Source / Originator**: Archana's Kitchen / Open Data repositories (Kaggle / GitHub open culinary datasets).
- **Stated License**: Creative Commons Attribution 4.0 International (CC BY 4.0) / Public Educational Research.
- **Permitted Use**: Research, academic capstone development, data analysis, transformation, and educational application development.
- **Attribution**: Original recipe text and culinary metadata credited to Archana's Kitchen contributors.
- **Role in InChef**: Primary source for Indian regional dishes, authentic recipe titles, regional cuisines (Bengali, South Indian, North Indian, Punjabi, Gujarati, etc.), prep/cook/total times, and translated instructions.

---

## 2. CulinaryDB (`01_Recipe_Details.csv`, `02_Ingredients.csv`, `03_Compound_Ingredients.csv`, `04_Recipe-Ingredients_Aliases.csv`)
- **Primary Source / Originator**: Center for Computational Biology, IIIT Delhi (Baghler et al., *CulinaryDB: A database of food recipes*, 2017).
- **Stated License**: Non-Commercial Research & Educational Use License.
- **Permitted Use**: Scientific research, educational modeling, computational gastronomy, and academic projects.
- **Attribution**: "CulinaryDB: A database of food recipes", IIIT Delhi.
- **Role in InChef**: Supporting ontological knowledge base for ingredient taxonomy, canonical naming, category classification, alias mapping, and compound ingredient composition.

---

## 3. DataHive (`recipes-with-nutrition(DataHive).csv`)
- **Primary Source / Originator**: DataHive Marketplace / Edamam API Aggregation.
- **Stated License**: **DataHive Custom License / Commercial API Restricted**.
- **Important Restrictions**:
  - The dataset contains nutrition breakdowns and health labels indexed from third-party web recipe sources.
  - The combined dataset **DOES NOT** carry unrestricted commercial redistribution rights.
  - Usage within this repository is restricted to **Capstone Project / Educational & Research Demonstration**.
  - Raw source datasets must remain confidential and are not redistributed or made publicly downloadable outside local project development.
- **Role in InChef**: Expansion source for East Asian and Southeast Asian recipes (Chinese, Japanese, Korean, Thai, Vietnamese, etc.), macro-nutrient information (calories, protein, carbohydrates, fat), dietary/allergen health labels, and image URL assets.

---

## 4. Derived & Processed Data Disclaimer
- Intermediate outputs in `data/cleaned/` and `data/processed/` are derived specifically for running InChef's intelligent recipe recommendation engine.
- No warranty is provided for recipe accuracy or allergen safety in medical/clinical settings; recommendations are for demonstration and culinary assistance purposes.
