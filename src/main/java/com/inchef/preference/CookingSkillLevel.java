package com.inchef.preference;

import com.inchef.recipe.DifficultyLevel;

public enum CookingSkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    public DifficultyLevel toDifficultyLevel() {
        return switch (this) {
            case BEGINNER -> DifficultyLevel.EASY;
            case INTERMEDIATE -> DifficultyLevel.MEDIUM;
            case ADVANCED -> DifficultyLevel.HARD;
        };
    }

    public static CookingSkillLevel fromDifficultyLevel(DifficultyLevel difficulty) {
        if (difficulty == null) return null;
        return switch (difficulty) {
            case EASY -> BEGINNER;
            case MEDIUM -> INTERMEDIATE;
            case HARD -> ADVANCED;
        };
    }
}
