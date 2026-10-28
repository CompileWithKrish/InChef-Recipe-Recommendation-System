package com.inchef.preference;

import java.time.LocalDateTime;
import java.util.List;

public class UserPreferenceResponse {
    private Long id;
    private Long userId;
    private DietaryType dietaryType;
    private Integer maxCookingTime;
    private List<String> preferredCuisines;
    private List<String> allergies;
    private SpiceLevel preferredSpiceLevel;
    private CookingSkillLevel cookingSkillLevel;
    private LocalDateTime updatedAt;

    public UserPreferenceResponse() {}

    public UserPreferenceResponse(Long id, Long userId, DietaryType dietaryType, Integer maxCookingTime, List<String> preferredCuisines, List<String> allergies, SpiceLevel preferredSpiceLevel, CookingSkillLevel cookingSkillLevel, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.dietaryType = dietaryType;
        this.maxCookingTime = maxCookingTime;
        this.preferredCuisines = preferredCuisines;
        this.allergies = allergies;
        this.preferredSpiceLevel = preferredSpiceLevel;
        this.cookingSkillLevel = cookingSkillLevel;
        this.updatedAt = updatedAt;
    }

    public UserPreferenceResponse(Long id, Long userId, DietaryType dietaryType, Integer maxCookingTime, List<String> preferredCuisines, List<String> allergies, SpiceLevel preferredSpiceLevel, LocalDateTime updatedAt) {
        this(id, userId, dietaryType, maxCookingTime, preferredCuisines, allergies, preferredSpiceLevel, null, updatedAt);
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return this.userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
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

    public CookingSkillLevel getCookingSkillLevel() {
        return this.cookingSkillLevel;
    }
    public void setCookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
        this.cookingSkillLevel = cookingSkillLevel;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private DietaryType dietaryType;
        private Integer maxCookingTime;
        private List<String> preferredCuisines;
        private List<String> allergies;
        private SpiceLevel preferredSpiceLevel;
        private CookingSkillLevel cookingSkillLevel;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

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

        public Builder preferredSpiceLevel(SpiceLevel preferredSpiceLevel) {
            this.preferredSpiceLevel = preferredSpiceLevel;
            return this;
        }

        public Builder cookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
            this.cookingSkillLevel = cookingSkillLevel;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserPreferenceResponse build() {
            UserPreferenceResponse obj = new UserPreferenceResponse();
            obj.setId(this.id);
            obj.setUserId(this.userId);
            obj.setDietaryType(this.dietaryType);
            obj.setMaxCookingTime(this.maxCookingTime);
            obj.setPreferredCuisines(this.preferredCuisines);
            obj.setAllergies(this.allergies);
            obj.setPreferredSpiceLevel(this.preferredSpiceLevel);
            obj.setCookingSkillLevel(this.cookingSkillLevel);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
