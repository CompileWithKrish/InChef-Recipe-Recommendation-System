package com.inchef.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class DishRecommendationRequest {

    private Long userId;

    @NotBlank(message = "Dish name or search query is required")
    private String dishName;

    private List<String> userIngredients = new ArrayList<>();

    private Boolean usePantry = true;

    private Integer limit = 10;

    public DishRecommendationRequest() {}

    public DishRecommendationRequest(Long userId, String dishName, List<String> userIngredients, Boolean usePantry, Integer limit) {
        this.userId = userId;
        this.dishName = dishName;
        this.userIngredients = userIngredients;
        this.usePantry = usePantry;
        this.limit = limit;
    }

    public Long getUserId() {
        return this.userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDishName() {
        return this.dishName;
    }
    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public List<String> getUserIngredients() {
        return this.userIngredients;
    }
    public void setUserIngredients(List<String> userIngredients) {
        this.userIngredients = userIngredients;
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
        private String dishName;
        private List<String> userIngredients = new ArrayList<>();
        private Boolean usePantry = true;
        private Integer limit = 10;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder dishName(String dishName) {
            this.dishName = dishName;
            return this;
        }

        public Builder userIngredients(List<String> userIngredients) {
            this.userIngredients = userIngredients;
            return this;
        }

        public Builder usePantry(Boolean usePantry) {
            this.usePantry = usePantry;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public DishRecommendationRequest build() {
            DishRecommendationRequest obj = new DishRecommendationRequest();
            obj.setUserId(this.userId);
            obj.setDishName(this.dishName);
            obj.setUserIngredients(this.userIngredients);
            obj.setUsePantry(this.usePantry);
            obj.setLimit(this.limit);
            return obj;
        }
    }
}
