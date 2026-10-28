package com.inchef.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum MeasurementUnit {
    GRAMS,
    KILOGRAMS,
    MILLILITERS,
    LITERS,
    CUPS,
    TABLESPOONS,
    TEASPOONS,
    PIECES,
    PINCH;

    @JsonCreator
    public static MeasurementUnit fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PIECES; // Safe documented default for pantry items
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "ITEM":
            case "ITEMS":
            case "PIECE":
            case "PCS":
            case "PC":
            case "PIECES":
                return PIECES;
            case "GRAM":
            case "GM":
            case "GMS":
            case "G":
            case "GRAMS":
                return GRAMS;
            case "KILOGRAM":
            case "KG":
            case "KGS":
            case "KILOGRAMS":
                return KILOGRAMS;
            case "MILLILITER":
            case "ML":
            case "MLS":
            case "MILLILITERS":
                return MILLILITERS;
            case "LITER":
            case "L":
            case "LTR":
            case "LITERS":
                return LITERS;
            case "CUP":
            case "CUPS":
                return CUPS;
            case "TABLESPOON":
            case "TBSP":
            case "TBS":
            case "TABLESPOONS":
                return TABLESPOONS;
            case "TEASPOON":
            case "TSP":
            case "TEASPOONS":
                return TEASPOONS;
            case "PINCH":
            case "PINCHES":
                return PINCH;
            default:
                try {
                    return MeasurementUnit.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid measurement unit '" + value + "'. Accepted units are: " +
                            Arrays.toString(MeasurementUnit.values()));
                }
        }
    }
}

