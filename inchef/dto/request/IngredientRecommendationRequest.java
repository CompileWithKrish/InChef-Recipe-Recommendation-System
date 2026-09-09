package com.inchef.dto.request;

import com.inchef.enums.DietaryType;

import java.util.ArrayList;
import java.util.List;

public class IngredientRecommendationRequest {

    private Long userId;

    private List<String> ingredients = new ArrayList<>();

    private Boolean usePantry = false;

    private DietaryType dietaryType;

    private String cuisine;

    private Integer maxCookingTime;

    private Integer limit = 10;

    public IngredientRecommendationRequest() {}

    public IngredientRecommendationRequest(Long userId, List<String> ingredients, Boolean usePantry, DietaryType dietaryType, String cuisine, Integer maxCookingTime, Integer limit) {
        this.userId = userId;
        this.ingredients = ingredients;
        this.usePantry = usePantry;
        this.dietaryType = dietaryType;
        this.cuisine = cuisine;
        this.maxCookingTime = maxCookingTime;
        this.limit = limit;
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

    public Integer getMaxCookingTime() {
        return this.maxCookingTime;
    }
    public void setMaxCookingTime(Integer maxCookingTime) {
        this.maxCookingTime = maxCookingTime;
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
        private Integer maxCookingTime;
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

        public Builder maxCookingTime(Integer maxCookingTime) {
            this.maxCookingTime = maxCookingTime;
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
            obj.setMaxCookingTime(this.maxCookingTime);
            obj.setLimit(this.limit);
            return obj;
        }
    }
}
