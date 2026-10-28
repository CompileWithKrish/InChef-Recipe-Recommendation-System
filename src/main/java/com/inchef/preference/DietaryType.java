package com.inchef.preference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DietaryType {
    VEGETARIAN,
    VEGAN,
    NON_VEGETARIAN,
    EGGETARIAN,
    GLUTEN_FREE,
    KETO,
    DAIRY_FREE;

    @JsonCreator
    public static DietaryType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toLowerCase().replaceAll("[\\-_\\s]+", "");
        if (clean.equals("all")) {
            return null; // Map All to no dietary restriction
        }
        if (clean.equals("vegetarian") || clean.equals("veg")) {
            return VEGETARIAN;
        }
        if (clean.equals("nonvegetarian") || clean.equals("nonveg")) {
            return NON_VEGETARIAN;
        }
        if (clean.equals("vegan")) {
            return VEGAN;
        }
        if (clean.equals("eggetarian") || clean.equals("egg") || clean.equals("eggitarian")) {
            return EGGETARIAN;
        }
        if (clean.equals("glutenfree")) {
            return GLUTEN_FREE;
        }
        if (clean.equals("keto") || clean.equals("ketogenic")) {
            return KETO;
        }
        if (clean.equals("dairyfree")) {
            return DAIRY_FREE;
        }
        throw new IllegalArgumentException("Invalid dietary type: '" + value + "'. Supported values: Vegetarian, Non-Vegetarian, Vegan, Eggetarian, Gluten-Free, Keto, Dairy-Free, or All.");
    }
}

