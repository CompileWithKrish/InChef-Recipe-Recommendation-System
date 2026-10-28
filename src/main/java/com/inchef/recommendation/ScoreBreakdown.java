package com.inchef.recommendation;

public class ScoreBreakdown {
    private double ingredientScore;          // Max 60
    private double ingredientMatchPercentage; // 0 to 100% coverage
    private double dietaryScore;             // Max 15
    private double cuisineScore;             // Max 10
    private double cookingTimeScore;         // Max 10
    private double categoryScore;            // Max 5
    private double skillScore;               // Skill compatibility bonus/penalty
    private double totalScore;

    public ScoreBreakdown() {}

    public ScoreBreakdown(double ingredientScore, double ingredientMatchPercentage, double dietaryScore, double cuisineScore, double cookingTimeScore, double categoryScore, double skillScore, double totalScore) {
        this.ingredientScore = ingredientScore;
        this.ingredientMatchPercentage = ingredientMatchPercentage;
        this.dietaryScore = dietaryScore;
        this.cuisineScore = cuisineScore;
        this.cookingTimeScore = cookingTimeScore;
        this.categoryScore = categoryScore;
        this.skillScore = skillScore;
        this.totalScore = totalScore;
    }

    public ScoreBreakdown(double ingredientScore, double dietaryScore, double cuisineScore, double cookingTimeScore, double categoryScore, double totalScore) {
        this(ingredientScore, 0.0, dietaryScore, cuisineScore, cookingTimeScore, categoryScore, 0.0, totalScore);
    }

    public double getIngredientScore() {
        return this.ingredientScore;
    }
    public void setIngredientScore(double ingredientScore) {
        this.ingredientScore = ingredientScore;
    }

    public double getIngredientMatchPercentage() {
        return this.ingredientMatchPercentage;
    }
    public void setIngredientMatchPercentage(double ingredientMatchPercentage) {
        this.ingredientMatchPercentage = ingredientMatchPercentage;
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

    public double getSkillScore() {
        return this.skillScore;
    }
    public void setSkillScore(double skillScore) {
        this.skillScore = skillScore;
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
        private double ingredientMatchPercentage;
        private double dietaryScore;
        private double cuisineScore;
        private double cookingTimeScore;
        private double categoryScore;
        private double skillScore;
        private double totalScore;

        public Builder ingredientScore(double ingredientScore) {
            this.ingredientScore = ingredientScore;
            return this;
        }

        public Builder ingredientMatchPercentage(double ingredientMatchPercentage) {
            this.ingredientMatchPercentage = ingredientMatchPercentage;
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

        public Builder skillScore(double skillScore) {
            this.skillScore = skillScore;
            return this;
        }

        public Builder totalScore(double totalScore) {
            this.totalScore = totalScore;
            return this;
        }

        public ScoreBreakdown build() {
            ScoreBreakdown obj = new ScoreBreakdown();
            obj.setIngredientScore(this.ingredientScore);
            obj.setIngredientMatchPercentage(this.ingredientMatchPercentage);
            obj.setDietaryScore(this.dietaryScore);
            obj.setCuisineScore(this.cuisineScore);
            obj.setCookingTimeScore(this.cookingTimeScore);
            obj.setCategoryScore(this.categoryScore);
            obj.setSkillScore(this.skillScore);
            obj.setTotalScore(this.totalScore);
            return obj;
        }
    }
}
