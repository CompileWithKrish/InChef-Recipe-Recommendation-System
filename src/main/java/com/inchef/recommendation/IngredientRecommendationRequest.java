package com.inchef.recommendation;

import java.util.ArrayList;
import java.util.List;

import com.inchef.preference.CookingSkillLevel;
import com.inchef.preference.DietaryType;
import com.inchef.recipe.MealType;

public class IngredientRecommendationRequest {

    private Long userId;

    private List<String> ingredients = new ArrayList<>();

    private Boolean usePantry = false;

    private DietaryType dietaryType;

    private String cuisine;

    private MealType mealType;

    private Integer maxCookingTime;

    private CookingSkillLevel cookingSkillLevel;

    private Double minIngredientMatch;

    private Integer limit = 10;

    public IngredientRecommendationRequest() {}

    public IngredientRecommendationRequest(Long userId, List<String> ingredients, Boolean usePantry, DietaryType dietaryType, String cuisine, MealType mealType, Integer maxCookingTime, CookingSkillLevel cookingSkillLevel, Double minIngredientMatch, Integer limit) {
        this.userId = userId;
        this.ingredients = ingredients;
        this.usePantry = usePantry;
        this.dietaryType = dietaryType;
        this.cuisine = cuisine;
        this.mealType = mealType;
        this.maxCookingTime = maxCookingTime;
        this.cookingSkillLevel = cookingSkillLevel;
        this.minIngredientMatch = minIngredientMatch;
        this.limit = limit;
    }

    public IngredientRecommendationRequest(Long userId, List<String> ingredients, Boolean usePantry, DietaryType dietaryType, String cuisine, Integer maxCookingTime, CookingSkillLevel cookingSkillLevel, Double minIngredientMatch, Integer limit) {
        this(userId, ingredients, usePantry, dietaryType, cuisine, null, maxCookingTime, cookingSkillLevel, minIngredientMatch, limit);
    }

    public IngredientRecommendationRequest(Long userId, List<String> ingredients, Boolean usePantry, DietaryType dietaryType, String cuisine, Integer maxCookingTime, Integer limit) {
        this(userId, ingredients, usePantry, dietaryType, cuisine, null, maxCookingTime, null, null, limit);
    }

    public Long getUserId() {
        return this.userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getIngredients() {
        return this.ingredients;
    }
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public Boolean getUsePantry() {
        return this.usePantry;
    }
    public Boolean isUsePantry() {
        return this.usePantry;
    }
    public void setUsePantry(Boolean usePantry) {
        this.usePantry = usePantry;
    }

    public DietaryType getDietaryType() {
        return this.dietaryType;
    }
    public void setDietaryType(DietaryType dietaryType) {
        this.dietaryType = dietaryType;
    }

    public String getCuisine() {
        return this.cuisine;
    }
    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public MealType getMealType() {
        return this.mealType;
    }
    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }
    public MealType getCourse() {
        return this.mealType;
    }
    public void setCourse(Object course) {
        if (course == null) {
            this.mealType = null;
            return;
        }
        if (course instanceof MealType) {
            this.mealType = (MealType) course;
            return;
        }
        String str = course.toString().trim();
        if (str.isEmpty() || str.equalsIgnoreCase("all") || str.equalsIgnoreCase("no preference")) {
            this.mealType = null;
            return;
        }
        try {
            this.mealType = MealType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
    }

    public Integer getMaxCookingTime() {
        return this.maxCookingTime;
    }
    public void setMaxCookingTime(Integer maxCookingTime) {
        this.maxCookingTime = maxCookingTime;
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
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public Double getMinIngredientMatch() {
        return this.minIngredientMatch;
    }
    public void setMinIngredientMatch(Double minIngredientMatch) {
        this.minIngredientMatch = minIngredientMatch;
    }
    public Double getMinimumMatchPercentage() {
        return this.minIngredientMatch;
    }
    public void setMinimumMatchPercentage(Double minimumMatchPercentage) {
        this.minIngredientMatch = minimumMatchPercentage;
    }

    public Integer getLimit() {
        return this.limit;
    }
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private List<String> ingredients = new ArrayList<>();
        private Boolean usePantry = false;
        private DietaryType dietaryType;
        private String cuisine;
        private MealType mealType;
        private Integer maxCookingTime;
        private CookingSkillLevel cookingSkillLevel;
        private Double minIngredientMatch;
        private Integer limit = 10;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder ingredients(List<String> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public Builder usePantry(Boolean usePantry) {
            this.usePantry = usePantry;
            return this;
        }

        public Builder dietaryType(DietaryType dietaryType) {
            this.dietaryType = dietaryType;
            return this;
        }

        public Builder cuisine(String cuisine) {
            this.cuisine = cuisine;
            return this;
        }

        public Builder mealType(MealType mealType) {
            this.mealType = mealType;
            return this;
        }

        public Builder course(String course) {
            if (course == null || course.isBlank() || course.trim().equalsIgnoreCase("all") || course.trim().equalsIgnoreCase("no preference")) {
                this.mealType = null;
            } else {
                try {
                    this.mealType = MealType.valueOf(course.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            return this;
        }

        public Builder maxCookingTime(Integer maxCookingTime) {
            this.maxCookingTime = maxCookingTime;
            return this;
        }

        public Builder cookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
            this.cookingSkillLevel = cookingSkillLevel;
            return this;
        }

        public Builder minIngredientMatch(Double minIngredientMatch) {
            this.minIngredientMatch = minIngredientMatch;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public IngredientRecommendationRequest build() {
            IngredientRecommendationRequest obj = new IngredientRecommendationRequest();
            obj.setUserId(this.userId);
            obj.setIngredients(this.ingredients);
            obj.setUsePantry(this.usePantry);
            obj.setDietaryType(this.dietaryType);
            obj.setCuisine(this.cuisine);
            obj.setMealType(this.mealType);
            obj.setMaxCookingTime(this.maxCookingTime);
            obj.setCookingSkillLevel(this.cookingSkillLevel);
            obj.setMinIngredientMatch(this.minIngredientMatch);
            obj.setLimit(this.limit);
            return obj;
        }
    }
}
