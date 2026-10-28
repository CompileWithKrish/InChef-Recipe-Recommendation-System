package com.inchef.preference;

import jakarta.validation.constraints.Positive;
import java.util.List;

public class UserPreferenceRequest {
    private DietaryType dietaryType;
    @Positive(message = "Max cooking time must be positive")
    private Integer maxCookingTime;
    private List<String> preferredCuisines;
    private List<String> allergies;
    private SpiceLevel preferredSpiceLevel;
    private CookingSkillLevel cookingSkillLevel;

    public UserPreferenceRequest() {}

    public UserPreferenceRequest(DietaryType dietaryType, Integer maxCookingTime, List<String> preferredCuisines, List<String> allergies, SpiceLevel preferredSpiceLevel, CookingSkillLevel cookingSkillLevel) {
        this.dietaryType = dietaryType;
        this.maxCookingTime = maxCookingTime;
        this.preferredCuisines = preferredCuisines;
        this.allergies = allergies;
        this.preferredSpiceLevel = preferredSpiceLevel;
        this.cookingSkillLevel = cookingSkillLevel;
    }

    public DietaryType getDietaryType() {
        return this.dietaryType;
    }
    public void setDietaryType(DietaryType dietaryType) {
        this.dietaryType = dietaryType;
    }

    public Integer getMaxCookingTime() {
        return this.maxCookingTime;
    }
    public void setMaxCookingTime(Integer maxCookingTime) {
        this.maxCookingTime = maxCookingTime;
    }

    public List<String> getPreferredCuisines() {
        return this.preferredCuisines;
    }
    public void setPreferredCuisines(List<String> preferredCuisines) {
        this.preferredCuisines = preferredCuisines;
    }

    public void setFavoriteCuisines(List<String> favoriteCuisines) {
        if (this.preferredCuisines == null || this.preferredCuisines.isEmpty()) {
            this.preferredCuisines = favoriteCuisines;
        }
    }

    public List<String> getAllergies() {
        return this.allergies;
    }
    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public SpiceLevel getPreferredSpiceLevel() {
        return this.preferredSpiceLevel;
    }
    public void setPreferredSpiceLevel(SpiceLevel preferredSpiceLevel) {
        this.preferredSpiceLevel = preferredSpiceLevel;
    }

    public void setSpiceLevel(SpiceLevel spiceLevel) {
        if (this.preferredSpiceLevel == null) {
            this.preferredSpiceLevel = spiceLevel;
        }
    }

    public CookingSkillLevel getCookingSkillLevel() {
        return this.cookingSkillLevel;
    }
    public void setCookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
        this.cookingSkillLevel = cookingSkillLevel;
    }

    public void setSkillLevel(String skill) {
        if (skill != null && !skill.isBlank()) {
            try {
                this.cookingSkillLevel = CookingSkillLevel.valueOf(skill.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore or try alternate naming
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DietaryType dietaryType;
        private Integer maxCookingTime;
        private List<String> preferredCuisines;
        private List<String> allergies;
        private SpiceLevel preferredSpiceLevel;
        private CookingSkillLevel cookingSkillLevel;

        public Builder dietaryType(DietaryType dietaryType) {
            this.dietaryType = dietaryType;
            return this;
        }

        public Builder maxCookingTime(Integer maxCookingTime) {
            this.maxCookingTime = maxCookingTime;
            return this;
        }

        public Builder preferredCuisines(List<String> preferredCuisines) {
            this.preferredCuisines = preferredCuisines;
            return this;
        }

        public Builder allergies(List<String> allergies) {
            this.allergies = allergies;
            return this;
        }

        public Builder allergies(String allergies) {
            if (allergies != null && !allergies.trim().isEmpty()) {
                this.allergies = java.util.Arrays.asList(allergies.split(",\\s*"));
            }
            return this;
        }

        public Builder preferredSpiceLevel(SpiceLevel preferredSpiceLevel) {
            this.preferredSpiceLevel = preferredSpiceLevel;
            return this;
        }

        public Builder cookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
            this.cookingSkillLevel = cookingSkillLevel;
            return this;
        }

        public UserPreferenceRequest build() {
            UserPreferenceRequest obj = new UserPreferenceRequest();
            obj.setDietaryType(this.dietaryType);
            obj.setMaxCookingTime(this.maxCookingTime);
            obj.setPreferredCuisines(this.preferredCuisines);
            obj.setAllergies(this.allergies);
            obj.setPreferredSpiceLevel(this.preferredSpiceLevel);
            obj.setCookingSkillLevel(this.cookingSkillLevel);
            return obj;
        }
    }
}
