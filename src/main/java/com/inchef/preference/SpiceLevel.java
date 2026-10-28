package com.inchef.preference;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SpiceLevel {
    MILD,
    MEDIUM,
    SPICY,
    EXTRA_SPICY;

    @JsonCreator
    public static SpiceLevel fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return SpiceLevel.valueOf(clean);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid spice level: '" + value + "'. Supported values: Mild, Medium, Spicy, Extra Spicy.");
        }
    }
}
