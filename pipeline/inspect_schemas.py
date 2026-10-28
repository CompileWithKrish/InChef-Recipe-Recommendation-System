import os
import sys
import pandas as pd

RAW_DIR = os.environ.get(
    "RAW_DATASET_DIR",
    os.path.join(os.path.dirname(__file__), "..", "data", "samples")
)

print("--- 1. Inspecting IndianFoodDatasetXLS.xlsx ---")
indian_path = os.path.join(RAW_DIR, "IndianFoodDatasetXLS.xlsx")
xl = pd.ExcelFile(indian_path)
print(f"Sheet names: {xl.sheet_names}")
df_indian = pd.read_excel(indian_path, sheet_name=0, nrows=10)
print(f"Indian columns ({len(df_indian.columns)}): {list(df_indian.columns)}")
print("Sample row 0:")
for col in df_indian.columns:
    print(f"  {col}: {repr(str(df_indian[col].iloc[0])[:100])}")

print("\n--- 2. Inspecting CulinaryDB ---")
culinary_dir = os.path.join(RAW_DIR, "CulinaryDB")
for fname in sorted(os.listdir(culinary_dir)):
    if fname.endswith(".csv"):
        fpath = os.path.join(culinary_dir, fname)
        df_c = pd.read_csv(fpath, nrows=5)
        print(f"\n{fname} columns ({len(df_c.columns)}): {list(df_c.columns)}")
        print(f"Sample row 0: {df_c.iloc[0].to_dict()}")

print("\n--- 3. Inspecting recipes-with-nutrition(DataHive).csv ---")
datahive_path = os.path.join(RAW_DIR, "recipes-with-nutrition(DataHive).csv")
df_dh = pd.read_csv(datahive_path, nrows=5)
print(f"DataHive columns ({len(df_dh.columns)}): {list(df_dh.columns)}")
print("Sample row 0:")
for col in df_dh.columns:
    print(f"  {col}: {repr(str(df_dh[col].iloc[0])[:120])}")
