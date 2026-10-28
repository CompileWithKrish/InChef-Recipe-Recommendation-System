#!/usr/bin/env python3
"""
InChef — Dataset Audit & Validation Tool
Profiles recipe dataset (Excel/CSV) for data quality, missing fields, time anomalies, and cuisine distributions.
"""

import os
import sys
import argparse
import pandas as pd

def main():
    parser = argparse.ArgumentParser(description="InChef Dataset Quality Audit Tool")
    parser.add_argument("--dataset", "-d", help="Path to Excel dataset (.xlsx or .csv)")
    args = parser.parse_args()

    dataset_path = args.dataset or os.environ.get("INCHEF_DATASET_PATH")
    if not dataset_path or not os.path.exists(dataset_path):
        sample_path = os.path.join("data", "samples", "sample_recipes.xlsx")
        if os.path.exists(sample_path):
            dataset_path = sample_path
        else:
            print("Error: Dataset not found.")
            sys.exit(1)

    print(f"==================================================")
    print(f" InChef Dataset Audit: {dataset_path}")
    print(f"==================================================")

    if dataset_path.endswith('.csv'):
        df = pd.read_csv(dataset_path)
    else:
        df = pd.read_excel(dataset_path)

    total_rows = len(df)
    print(f"Total Records: {total_rows}")

    # Required columns check
    expected_cols = ['RecipeName', 'Ingredients', 'PrepTimeInMins', 'CookTimeInMins', 'Cuisine', 'Instructions']
    missing_cols = [c for c in expected_cols if c not in df.columns]
    if missing_cols:
        print(f"WARNING: Missing expected columns: {missing_cols}")
    else:
        print("All expected core columns present.")

    # Null value profile
    print("\nNull Values per Core Column:")
    for col in df.columns:
        null_count = df[col].isnull().sum()
        if null_count > 0:
            print(f"  - {col}: {null_count} ({null_count/total_rows*100:.1f}%)")

    # Duplicates check
    if 'RecipeName' in df.columns:
        dup_names = df['RecipeName'].duplicated().sum()
        print(f"\nDuplicate Recipe Names: {dup_names}")

    # Time anomalies check
    if 'TotalTimeInMins' in df.columns:
        zero_time = (df['TotalTimeInMins'] <= 0).sum()
        huge_time = (df['TotalTimeInMins'] > 720).sum()
        print(f"\nCooking Time Anomalies:")
        print(f"  - Non-positive TotalTime: {zero_time}")
        print(f"  - Excessively long TotalTime (>12 hrs): {huge_time}")

    # Cuisine distribution
    if 'Cuisine' in df.columns:
        print("\nTop 10 Cuisines:")
        for cuisine, count in df['Cuisine'].value_counts().head(10).items():
            print(f"  - {cuisine}: {count}")

    # Diet distribution
    if 'Diet' in df.columns:
        print("\nDietary Breakdown:")
        for diet, count in df['Diet'].value_counts().items():
            print(f"  - {diet}: {count}")

    print("\n==================================================")
    print(" Audit Completed Successfully.")
    print("==================================================")

if __name__ == '__main__':
    main()

