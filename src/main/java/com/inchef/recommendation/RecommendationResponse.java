package com.inchef.recommendation;

import java.util.ArrayList;
import java.util.List;

import com.inchef.recipe.RecipeDetailResponse;

public class RecommendationResponse {
    private RecipeDetailResponse recipe;
    private int matchScore; // 0 to 100 percentage
    private double ingredientMatchPercentage; // actual ingredient coverage %

    private List<String> matchedIngredients = new ArrayList<>();
    private List<String> missingIngredients = new ArrayList<>();
    private String reason;
    private ScoreBreakdown scoreBreakdown;

    public RecommendationResponse() {}

    public RecommendationResponse(RecipeDetailResponse recipe, int matchScore, double ingredientMatchPercentage, List<String> matchedIngredients, List<String> missingIngredients, String reason, ScoreBreakdown scoreBreakdown) {
        this.recipe = recipe;
        this.matchScore = matchScore;
        this.ingredientMatchPercentage = ingredientMatchPercentage;
        this.matchedIngredients = matchedIngredients;
        this.missingIngredients = missingIngredients;
        this.reason = reason;
        this.scoreBreakdown = scoreBreakdown;
    }

    public RecommendationResponse(RecipeDetailResponse recipe, int matchScore, List<String> matchedIngredients, List<String> missingIngredients, String reason, ScoreBreakdown scoreBreakdown) {
        this(recipe, matchScore, (double) matchScore, matchedIngredients, missingIngredients, reason, scoreBreakdown);
    }

    public RecipeDetailResponse getRecipe() {
        return this.recipe;
    }
    public void setRecipe(RecipeDetailResponse recipe) {
        this.recipe = recipe;
    }

    public int getMatchScore() {
        return this.matchScore;
    }
    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public double getIngredientMatchPercentage() {
        return this.ingredientMatchPercentage;
    }
    public void setIngredientMatchPercentage(double ingredientMatchPercentage) {
        this.ingredientMatchPercentage = ingredientMatchPercentage;
    }

    public List<String> getMatchedIngredients() {
        return this.matchedIngredients;
    }
    public void setMatchedIngredients(List<String> matchedIngredients) {
        this.matchedIngredients = matchedIngredients;
    }

    public List<String> getMissingIngredients() {
        return this.missingIngredients;
    }
    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }

    public String getReason() {
        return this.reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public ScoreBreakdown getScoreBreakdown() {
        return this.scoreBreakdown;
    }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RecipeDetailResponse recipe;
        private int matchScore;
        private double ingredientMatchPercentage;
        private List<String> matchedIngredients = new ArrayList<>();
        private List<String> missingIngredients = new ArrayList<>();
        private String reason;
        private ScoreBreakdown scoreBreakdown;

        public Builder recipe(RecipeDetailResponse recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder matchScore(int matchScore) {
            this.matchScore = matchScore;
            return this;
        }

        public Builder ingredientMatchPercentage(double ingredientMatchPercentage) {
            this.ingredientMatchPercentage = ingredientMatchPercentage;
            return this;
        }

        public Builder matchedIngredients(List<String> matchedIngredients) {
            this.matchedIngredients = matchedIngredients;
            return this;
        }

        public Builder missingIngredients(List<String> missingIngredients) {
            this.missingIngredients = missingIngredients;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder scoreBreakdown(ScoreBreakdown scoreBreakdown) {
            this.scoreBreakdown = scoreBreakdown;
            return this;
        }

        public RecommendationResponse build() {
            RecommendationResponse obj = new RecommendationResponse();
            obj.setRecipe(this.recipe);
            obj.setMatchScore(this.matchScore);
            obj.setIngredientMatchPercentage(this.ingredientMatchPercentage);
            obj.setMatchedIngredients(this.matchedIngredients);
            obj.setMissingIngredients(this.missingIngredients);
            obj.setReason(this.reason);
            obj.setScoreBreakdown(this.scoreBreakdown);
            return obj;
        }
    }
}
