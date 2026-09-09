package com.inchef.service;

import com.inchef.dto.request.DishRecommendationRequest;
import com.inchef.dto.request.IngredientRecommendationRequest;
import com.inchef.dto.response.RecipeDetailResponse;
import com.inchef.dto.response.RecommendationResponse;
import com.inchef.dto.response.UserIngredientResponse;
import com.inchef.dto.response.UserPreferenceResponse;
import com.inchef.entity.Recipe;
import com.inchef.entity.Recommendation;
import com.inchef.entity.User;
import com.inchef.enums.RecommendationMode;
import com.inchef.repository.RecipeRepository;
import com.inchef.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecommendationService {

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final UserIngredientService userIngredientService;
    private final UserPreferenceService userPreferenceService;
    private final UserService userService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationEngine recommendationEngine;

    public RecommendationService(RecipeRepository recipeRepository,
                                 RecipeService recipeService,
                                 UserIngredientService userIngredientService,
                                 UserPreferenceService userPreferenceService,
                                 UserService userService,
                                 RecommendationRepository recommendationRepository,
                                 RecommendationEngine recommendationEngine) {
        this.recipeRepository = recipeRepository;
        this.recipeService = recipeService;
        this.userIngredientService = userIngredientService;
        this.userPreferenceService = userPreferenceService;
        this.userService = userService;
        this.recommendationRepository = recommendationRepository;
        this.recommendationEngine = recommendationEngine;
    }

    public List<RecommendationResponse> recommendByIngredients(IngredientRecommendationRequest request) {
        Set<String> normalizedIngredients = new HashSet<>();

        // 1. Add explicitly provided ingredients
        if (request.getIngredients() != null) {
            for (String ing : request.getIngredients()) {
                if (ing != null && !ing.trim().isEmpty()) {
                    normalizedIngredients.add(ing.trim().toLowerCase());
                }
            }
        }

        // 2. Add user pantry ingredients if requested or user specified
        User user = null;
        RecommendationEngine.PreferenceContext prefContext = new RecommendationEngine.PreferenceContext();

        if (request.getUserId() != null) {
            user = userService.findEntityById(request.getUserId());
            if (Boolean.TRUE.equals(request.getUsePantry())) {
                List<UserIngredientResponse> pantry = userIngredientService.getUserIngredients(request.getUserId());
                for (UserIngredientResponse item : pantry) {
                    normalizedIngredients.add(item.getIngredientName().trim().toLowerCase());
                }
            }

            // Load user preferences
            UserPreferenceResponse userPref = userPreferenceService.getUserPreference(request.getUserId());
            prefContext.dietaryType = userPref.getDietaryType();
            prefContext.preferredCuisines = userPref.getPreferredCuisines() != null ? new ArrayList<>(userPref.getPreferredCuisines()) : new ArrayList<>();
            prefContext.allergies = userPref.getAllergies() != null ? new ArrayList<>(userPref.getAllergies()) : new ArrayList<>();
            prefContext.maxCookingTime = userPref.getMaxCookingTime();
        }

        // Apply request overrides
        if (request.getDietaryType() != null) {
            prefContext.dietaryType = request.getDietaryType();
        }
        if (request.getCuisine() != null && !request.getCuisine().isBlank()) {
            prefContext.preferredCuisines = Collections.singletonList(request.getCuisine().trim());
        }
        if (request.getMaxCookingTime() != null) {
            prefContext.maxCookingTime = request.getMaxCookingTime();
        }

        List<Recipe> allRecipes = recipeRepository.findAllPublishedWithIngredients();
        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
            // Check allergy exclusions
            if (hasAllergyConflict(recipe, prefContext.allergies)) {
                continue;
            }

            RecipeDetailResponse detail = recipeService.mapToDetailResponse(recipe, request.getUserId());
            RecommendationResponse rec = recommendationEngine.evaluateRecipe(
                    recipe,
                    normalizedIngredients,
                    prefContext,
                    detail
            );

            // Keep if has at least some match or reasonable score
            if (rec.getMatchScore() > 0 || !rec.getMatchedIngredients().isEmpty()) {
                recommendations.add(rec);
            }
        }

        // Sort descending by match score, then by total time
        recommendations.sort(Comparator.comparingInt(RecommendationResponse::getMatchScore).reversed()
                .thenComparing(r -> r.getRecipe().getTotalTimeMinutes() != null ? r.getRecipe().getTotalTimeMinutes() : 999));

        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 10;
        List<RecommendationResponse> limited = recommendations.stream().limit(limit).collect(Collectors.toList());

        // Log recommendation query
        logRecommendation(user, RecommendationMode.INGREDIENT_BASED,
                String.join(", ", normalizedIngredients), limited.size());

        return limited;
    }

    public List<RecommendationResponse> recommendByDish(DishRecommendationRequest request) {
        String dishName = request.getDishName().trim();
        List<Recipe> matchingRecipes = recipeRepository.findByTitleContainingIgnoreCaseWithIngredients(dishName);

        // Fallback to all published recipes if no direct name match
        if (matchingRecipes.isEmpty()) {
            matchingRecipes = recipeRepository.findAllPublishedWithIngredients();
        }

        Set<String> normalizedIngredients = new HashSet<>();
        if (request.getUserIngredients() != null) {
            for (String ing : request.getUserIngredients()) {
                if (ing != null && !ing.trim().isEmpty()) {
                    normalizedIngredients.add(ing.trim().toLowerCase());
                }
            }
        }

        User user = null;
        RecommendationEngine.PreferenceContext prefContext = new RecommendationEngine.PreferenceContext();

        if (request.getUserId() != null) {
            user = userService.findEntityById(request.getUserId());
            if (Boolean.TRUE.equals(request.getUsePantry())) {
                List<UserIngredientResponse> pantry = userIngredientService.getUserIngredients(request.getUserId());
                for (UserIngredientResponse item : pantry) {
                    normalizedIngredients.add(item.getIngredientName().trim().toLowerCase());
                }
            }

            UserPreferenceResponse userPref = userPreferenceService.getUserPreference(request.getUserId());
            prefContext.dietaryType = userPref.getDietaryType();
            prefContext.preferredCuisines = userPref.getPreferredCuisines() != null ? new ArrayList<>(userPref.getPreferredCuisines()) : new ArrayList<>();
            prefContext.allergies = userPref.getAllergies() != null ? new ArrayList<>(userPref.getAllergies()) : new ArrayList<>();
            prefContext.maxCookingTime = userPref.getMaxCookingTime();
        }

        List<RecommendationResponse> recommendations = new ArrayList<>();
        for (Recipe recipe : matchingRecipes) {
            if (hasAllergyConflict(recipe, prefContext.allergies)) {
                continue;
            }

            RecipeDetailResponse detail = recipeService.mapToDetailResponse(recipe, request.getUserId());
            RecommendationResponse rec = recommendationEngine.evaluateRecipe(
                    recipe,
                    normalizedIngredients,
                    prefContext,
                    detail
            );
            recommendations.add(rec);
        }

        // Sort by match score descending
        recommendations.sort(Comparator.comparingInt(RecommendationResponse::getMatchScore).reversed());

        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 10;
        List<RecommendationResponse> limited = recommendations.stream().limit(limit).collect(Collectors.toList());

        // Log recommendation query
        logRecommendation(user, RecommendationMode.DISH_BASED, dishName, limited.size());

        return limited;
    }

    private boolean hasAllergyConflict(Recipe recipe, List<String> allergies) {
        if (allergies == null || allergies.isEmpty() || recipe.getRecipeIngredients() == null) {
            return false;
        }

        for (var ri : recipe.getRecipeIngredients()) {
            String ingName = ri.getIngredient().getName().toLowerCase();
            for (String allergy : allergies) {
                if (allergy != null && !allergy.isBlank()) {
                    String cleanAllergy = allergy.trim().toLowerCase();
                    if (ingName.contains(cleanAllergy) || cleanAllergy.contains(ingName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void logRecommendation(User user, RecommendationMode mode, String inputQuery, int count) {
        Recommendation log = Recommendation.builder()
                .user(user)
                .mode(mode)
                .inputQuery(inputQuery)
                .recommendedRecipesCount(count)
                .build();
        recommendationRepository.save(log);
    }
}
