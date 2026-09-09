package com.inchef.dto.response;


public class ScoreBreakdown {
    private double ingredientScore;  // Max 60
    private double dietaryScore;     // Max 15
    private double cuisineScore;     // Max 10
    private double cookingTimeScore; // Max 10
    private double categoryScore;    // Max 5
    private double totalScore;

    public ScoreBreakdown() {}

    public ScoreBreakdown(double ingredientScore, double dietaryScore, double cuisineScore, double cookingTimeScore, double categoryScore, double totalScore) {
        this.ingredientScore = ingredientScore;
        this.dietaryScore = dietaryScore;
        this.cuisineScore = cuisineScore;
        this.cookingTimeScore = cookingTimeScore;
        this.categoryScore = categoryScore;
        this.totalScore = totalScore;
    }

    public double getIngredientScore() {
        return this.ingredientScore;
    }
    public void setIngredientScore(double ingredientScore) {
        this.ingredientScore = ingredientScore;
    }

    public double getDietaryScore() {
        return this.dietaryScore;
    }
    public void setDietaryScore(double dietaryScore) {
        this.dietaryScore = dietaryScore;
    }

    public double getCuisineScore() {
        return this.cuisineScore;
    }
    public void setCuisineScore(double cuisineScore) {
        this.cuisineScore = cuisineScore;
    }

    public double getCookingTimeScore() {
        return this.cookingTimeScore;
    }
    public void setCookingTimeScore(double cookingTimeScore) {
        this.cookingTimeScore = cookingTimeScore;
    }

    public double getCategoryScore() {
        return this.categoryScore;
    }
    public void setCategoryScore(double categoryScore) {
        this.categoryScore = categoryScore;
    }

    public double getTotalScore() {
        return this.totalScore;
    }
    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double ingredientScore;
        private double dietaryScore;
        private double cuisineScore;
        private double cookingTimeScore;
        private double categoryScore;
        private double totalScore;

        public Builder ingredientScore(double ingredientScore) {
            this.ingredientScore = ingredientScore;
            return this;
        }

        public Builder dietaryScore(double dietaryScore) {
            this.dietaryScore = dietaryScore;
            return this;
        }

        public Builder cuisineScore(double cuisineScore) {
            this.cuisineScore = cuisineScore;
            return this;
        }

        public Builder cookingTimeScore(double cookingTimeScore) {
            this.cookingTimeScore = cookingTimeScore;
            return this;
        }

        public Builder categoryScore(double categoryScore) {
            this.categoryScore = categoryScore;
            return this;
        }

        public Builder totalScore(double totalScore) {
            this.totalScore = totalScore;
            return this;
        }

        public ScoreBreakdown build() {
            ScoreBreakdown obj = new ScoreBreakdown();
            obj.setIngredientScore(this.ingredientScore);
            obj.setDietaryScore(this.dietaryScore);
            obj.setCuisineScore(this.cuisineScore);
            obj.setCookingTimeScore(this.cookingTimeScore);
            obj.setCategoryScore(this.categoryScore);
            obj.setTotalScore(this.totalScore);
            return obj;
        }
    }
}
