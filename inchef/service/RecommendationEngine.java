package com.inchef.service;

import com.inchef.dto.response.RecipeDetailResponse;
import com.inchef.dto.response.RecipeIngredientResponse;
import com.inchef.dto.response.RecommendationResponse;
import com.inchef.dto.response.ScoreBreakdown;
import com.inchef.entity.Recipe;
import com.inchef.enums.DietaryType;
import com.inchef.enums.MealType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationEngine {

    public static class PreferenceContext {
        public DietaryType dietaryType;
        public List<String> preferredCuisines = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();
        public Integer maxCookingTime;
        public MealType mealType;
    }

    public RecommendationResponse evaluateRecipe(
            Recipe recipe,
            Set<String> normalizedUserIngredients,
            PreferenceContext preferences,
            RecipeDetailResponse recipeDetail) {

        List<RecipeIngredientResponse> recipeIngredients = recipeDetail.getIngredients() != null
                ? recipeDetail.getIngredients()
                : Collections.emptyList();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (RecipeIngredientResponse rir : recipeIngredients) {
            String ingName = rir.getIngredientName().toLowerCase().trim();
            boolean isMatched = isIngredientMatched(ingName, normalizedUserIngredients);

            if (isMatched) {
                matched.add(rir.getIngredientName());
            } else {
                missing.add(rir.getIngredientName());
            }
        }

        int totalIngredients = Math.max(1, recipeIngredients.size());
        double ingredientRatio = (double) matched.size() / totalIngredients;
        double ingredientScore = Math.round(ingredientRatio * 60.0 * 10.0) / 10.0;

        // Dietary Score (Max 15)
        double dietaryScore = calculateDietaryScore(recipe.getDietaryType(), preferences.dietaryType);

        // Cuisine Score (Max 10)
        double cuisineScore = calculateCuisineScore(recipe.getCuisine(), preferences.preferredCuisines);

        // Cooking Time Score (Max 10)
        double cookingTimeScore = calculateTimeScore(recipe.getTotalTimeMinutes(), preferences.maxCookingTime);

        // Category / Meal Type Score (Max 5)
        double categoryScore = calculateMealTypeScore(recipe.getMealType(), preferences.mealType);

        double totalScore = ingredientScore + dietaryScore + cuisineScore + cookingTimeScore + categoryScore;
        totalScore = Math.min(100.0, Math.max(0.0, Math.round(totalScore * 10.0) / 10.0));

        ScoreBreakdown breakdown = ScoreBreakdown.builder()
                .ingredientScore(ingredientScore)
                .dietaryScore(dietaryScore)
                .cuisineScore(cuisineScore)
                .cookingTimeScore(cookingTimeScore)
                .categoryScore(categoryScore)
                .totalScore(totalScore)
                .build();

        String reason = generateReason(matched.size(), totalIngredients, recipe, preferences, totalScore);

        return RecommendationResponse.builder()
                .recipe(recipeDetail)
                .matchScore((int) Math.round(totalScore))
                .matchedIngredients(matched)
                .missingIngredients(missing)
                .reason(reason)
                .scoreBreakdown(breakdown)
                .build();
    }

    private boolean isIngredientMatched(String recipeIngredient, Set<String> userIngredients) {
        if (userIngredients.contains(recipeIngredient)) {
            return true;
        }
        for (String userIng : userIngredients) {
            if (userIng.isEmpty()) continue;
            // Token/contains matching: e.g. "tomato" in "fresh tomato", "chicken breast" matches "chicken"
            if (recipeIngredient.contains(userIng) || userIng.contains(recipeIngredient)) {
                return true;
            }
        }
        return false;
    }

    private double calculateDietaryScore(DietaryType recipeDiet, DietaryType userDiet) {
        if (userDiet == null || userDiet == DietaryType.NON_VEGETARIAN) {
            return 15.0; // Any diet is acceptable
        }
        if (recipeDiet == userDiet) {
            return 15.0;
        }
        // Vegan recipes satisfy Vegetarian
        if (userDiet == DietaryType.VEGETARIAN && recipeDiet == DietaryType.VEGAN) {
            return 15.0;
        }
        // Vegan or Vegetarian recipes satisfy Eggetarian
        if (userDiet == DietaryType.EGGETARIAN &&
                (recipeDiet == DietaryType.VEGETARIAN || recipeDiet == DietaryType.VEGAN || recipeDiet == DietaryType.EGGETARIAN)) {
            return 15.0;
        }
        return 0.0;
    }

    private double calculateCuisineScore(String recipeCuisine, List<String> preferredCuisines) {
        if (preferredCuisines == null || preferredCuisines.isEmpty()) {
            return 7.0; // neutral score when no preference stated
        }
        if (recipeCuisine == null) {
            return 5.0;
        }
        for (String pref : preferredCuisines) {
            if (pref.trim().equalsIgnoreCase(recipeCuisine.trim())) {
                return 10.0;
            }
        }
        return 2.0;
    }

    private double calculateTimeScore(Integer recipeTimeMinutes, Integer maxCookingTime) {
        if (maxCookingTime == null || maxCookingTime <= 0) {
            return 10.0; // no constraint
        }
        int actualTime = recipeTimeMinutes != null ? recipeTimeMinutes : 30;
        if (actualTime <= maxCookingTime) {
            return 10.0;
        }
        // Deduct points for excess time
        double penalty = (actualTime - maxCookingTime) * 0.2;
        return Math.max(0.0, Math.round((10.0 - penalty) * 10.0) / 10.0);
    }

    private double calculateMealTypeScore(MealType recipeMeal, MealType requestedMeal) {
        if (requestedMeal == null || recipeMeal == requestedMeal) {
            return 5.0;
        }
        return 2.5;
    }

    private String generateReason(int matchedCount, int totalCount, Recipe recipe, PreferenceContext prefs, double totalScore) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Matched %d of %d ingredients (%.0f%%).",
                matchedCount, totalCount, ((double) matchedCount / totalCount) * 100));

        if (prefs.dietaryType != null && prefs.dietaryType == recipe.getDietaryType()) {
            sb.append(String.format(" Matches your %s preference.", recipe.getDietaryType()));
        }
        if (recipe.getCuisine() != null && prefs.preferredCuisines != null &&
                prefs.preferredCuisines.stream().anyMatch(c -> c.equalsIgnoreCase(recipe.getCuisine()))) {
            sb.append(String.format(" Aligns with your %s cuisine preference.", recipe.getCuisine()));
        }
        if (recipe.getTotalTimeMinutes() != null && recipe.getTotalTimeMinutes() <= 30) {
            sb.append(String.format(" Quick to prepare in %d mins.", recipe.getTotalTimeMinutes()));
        }

        return sb.toString();
    }
}
