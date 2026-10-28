package com.inchef.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import com.inchef.pantry.UserIngredientResponse;
import com.inchef.pantry.UserIngredientService;
import com.inchef.preference.CookingSkillLevel;
import com.inchef.preference.DietaryType;
import com.inchef.preference.UserPreferenceResponse;
import com.inchef.preference.UserPreferenceService;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeDetailResponse;
import com.inchef.recipe.RecipeIngredientResponse;
import com.inchef.recipe.RecipeRepository;
import com.inchef.recipe.RecipeService;
import com.inchef.user.User;
import com.inchef.user.UserService;

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

    @Value("${inchef.recommendations.minimum-match-percentage:${recommendation.min-ingredient-match:75.0}}")
    private double defaultMinIngredientMatch = 75.0;

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

        // 2. Add user pantry ingredients if requested
        User user = null;
        RecommendationEngine.PreferenceContext prefContext = new RecommendationEngine.PreferenceContext();

        if (request.getUserId() != null) {
            user = userService.findEntityById(request.getUserId());
            if (Boolean.TRUE.equals(request.getUsePantry())) {
                List<UserIngredientResponse> pantry = userIngredientService.getUserIngredients(request.getUserId());
                for (UserIngredientResponse item : pantry) {
                    if (item.getIngredientName() != null && !item.getIngredientName().trim().isEmpty()) {
                        normalizedIngredients.add(item.getIngredientName().trim().toLowerCase());
                    }
                }
            }

            // Load user preferences
            UserPreferenceResponse userPref = userPreferenceService.getUserPreference(request.getUserId());
            prefContext.dietaryType = userPref.getDietaryType();
            prefContext.preferredCuisines = userPref.getPreferredCuisines() != null ? new ArrayList<>(userPref.getPreferredCuisines()) : new ArrayList<>();
            prefContext.allergies = userPref.getAllergies() != null ? new ArrayList<>(userPref.getAllergies()) : new ArrayList<>();
            prefContext.maxCookingTime = userPref.getMaxCookingTime();
            prefContext.cookingSkillLevel = userPref.getCookingSkillLevel();
        }

        // Apply request overrides
        if (request.getDietaryType() != null) {
            prefContext.dietaryType = request.getDietaryType();
        }
        if (request.getCuisine() != null && !request.getCuisine().isBlank() && !request.getCuisine().trim().equalsIgnoreCase("all")) {
            prefContext.preferredCuisines = Collections.singletonList(request.getCuisine().trim());
        }
        if (request.getMealType() != null) {
            prefContext.mealType = request.getMealType();
        }
        if (request.getMaxCookingTime() != null) {
            prefContext.maxCookingTime = request.getMaxCookingTime();
        }
        if (request.getCookingSkillLevel() != null) {
            prefContext.cookingSkillLevel = request.getCookingSkillLevel();
        }

        double effectiveThreshold = request.getMinIngredientMatch() != null
                ? request.getMinIngredientMatch()
                : this.defaultMinIngredientMatch;

        List<Recipe> allRecipes = getAllPublishedRecipes();
        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
            // Safety: Never bypass allergy restrictions
            if (hasAllergyConflict(recipe, prefContext.allergies)) {
                continue;
            }

            // Personalization Filter 1: Dietary preference (only filter if specified)
            if (prefContext.dietaryType != null && hasDietaryConflict(recipe.getDietaryType(), prefContext.dietaryType)) {
                continue;
            }

            // Personalization Filter 2: Cuisine preference (only filter if specified)
            if (prefContext.preferredCuisines != null && !prefContext.preferredCuisines.isEmpty()
                    && !matchesCuisine(recipe.getCuisine(), prefContext.preferredCuisines)) {
                continue;
            }

            // Personalization Filter: Course / Meal type (only filter if specified)
            if (prefContext.mealType != null && recipe.getMealType() != prefContext.mealType) {
                continue;
            }

            // Personalization Filter 3: Maximum cooking time (only filter if specified)
            if (prefContext.maxCookingTime != null && prefContext.maxCookingTime > 0) {
                if (recipe.getTotalTimeMinutes() != null && recipe.getTotalTimeMinutes() > prefContext.maxCookingTime) {
                    continue;
                }
            }

            // Personalization Filter 4: Cooking skill compatibility (only filter if specified)
            if (prefContext.cookingSkillLevel != null
                    && !recommendationEngine.isSkillCompatible(recipe.getDifficulty(), prefContext.cookingSkillLevel)) {
                continue;
            }

            RecipeDetailResponse detail = mapRecipeForEvaluation(recipe);
            RecommendationResponse rec = recommendationEngine.evaluateRecipe(
                    recipe,
                    normalizedIngredients,
                    prefContext,
                    detail
            );

            // Core InChef Rule: Apply Configurable Ingredient Coverage Threshold (default 75%)
            if (normalizedIngredients.isEmpty() || rec.getIngredientMatchPercentage() >= effectiveThreshold) {
                recommendations.add(rec);
            }
        }

        // Deterministic ordering: descending match score, descending matched count, ascending total cooking time, ascending recipe id
        recommendations.sort(Comparator.comparingInt(RecommendationResponse::getMatchScore).reversed()
                .thenComparing(r -> r.getMatchedIngredients() != null ? r.getMatchedIngredients().size() : 0, Comparator.reverseOrder())
                .thenComparing(r -> r.getRecipe().getTotalTimeMinutes() != null ? r.getRecipe().getTotalTimeMinutes() : 999)
                .thenComparing(r -> r.getRecipe().getId() != null ? r.getRecipe().getId() : 0L));

        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 10;
        List<RecommendationResponse> limited = recommendations.stream().limit(limit).collect(Collectors.toList());

        // Enrich only the top results with full favorite status and count
        Map<Long, Recipe> recipeMap = allRecipes.stream().collect(Collectors.toMap(Recipe::getId, r -> r, (a, b) -> a));
        for (RecommendationResponse rr : limited) {
            Recipe r = recipeMap.get(rr.getRecipe().getId());
            if (r != null) {
                rr.setRecipe(recipeService.mapToDetailResponse(r, request.getUserId()));
            }
        }

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
            matchingRecipes = getAllPublishedRecipes();
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
                    if (item.getIngredientName() != null && !item.getIngredientName().trim().isEmpty()) {
                        normalizedIngredients.add(item.getIngredientName().trim().toLowerCase());
                    }
                }
            }

            UserPreferenceResponse userPref = userPreferenceService.getUserPreference(request.getUserId());
            prefContext.dietaryType = userPref.getDietaryType();
            prefContext.preferredCuisines = userPref.getPreferredCuisines() != null ? new ArrayList<>(userPref.getPreferredCuisines()) : new ArrayList<>();
            prefContext.allergies = userPref.getAllergies() != null ? new ArrayList<>(userPref.getAllergies()) : new ArrayList<>();
            prefContext.maxCookingTime = userPref.getMaxCookingTime();
            prefContext.cookingSkillLevel = userPref.getCookingSkillLevel();
        }

        List<RecommendationResponse> recommendations = new ArrayList<>();
        for (Recipe recipe : matchingRecipes) {
            // Safety: Never bypass allergy violations
            if (hasAllergyConflict(recipe, prefContext.allergies)) {
                continue;
            }

            // Exclude dietary violations
            if (hasDietaryConflict(recipe.getDietaryType(), prefContext.dietaryType)) {
                continue;
            }

            RecipeDetailResponse detail = mapRecipeForEvaluation(recipe);
            RecommendationResponse rec = recommendationEngine.evaluateRecipe(
                    recipe,
                    normalizedIngredients,
                    prefContext,
                    detail
            );
            recommendations.add(rec);
        }

        // If direct dish matches existed but were all filtered out by user dietary profile,
        // relax strict dietary filter so the user still receives the requested dish,
        // BUT STILL STRICTLY ENFORCE ALLERGIES!
        if (recommendations.isEmpty() && !matchingRecipes.isEmpty()) {
            for (Recipe recipe : matchingRecipes) {
                if (hasAllergyConflict(recipe, prefContext.allergies)) {
                    continue;
                }
                RecipeDetailResponse detail = mapRecipeForEvaluation(recipe);
                RecommendationResponse rec = recommendationEngine.evaluateRecipe(
                        recipe,
                        normalizedIngredients,
                        new RecommendationEngine.PreferenceContext(),
                        detail
                );
                recommendations.add(rec);
            }
        }

        // Sort descending by match score, then total cooking time ascending, then recipe ID ascending
        recommendations.sort(Comparator.comparingInt(RecommendationResponse::getMatchScore).reversed()
                .thenComparing(r -> r.getRecipe().getTotalTimeMinutes() != null ? r.getRecipe().getTotalTimeMinutes() : 999)
                .thenComparing(r -> r.getRecipe().getId() != null ? r.getRecipe().getId() : 0L));

        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 10;
        List<RecommendationResponse> limited = recommendations.stream().limit(limit).collect(Collectors.toList());

        // Enrich only the top results with full favorite status and count
        Map<Long, Recipe> recipeMap = matchingRecipes.stream().collect(Collectors.toMap(Recipe::getId, r -> r, (a, b) -> a));
        for (RecommendationResponse rr : limited) {
            Recipe r = recipeMap.get(rr.getRecipe().getId());
            if (r != null) {
                rr.setRecipe(recipeService.mapToDetailResponse(r, request.getUserId()));
            }
        }

        // Log recommendation query
        logRecommendation(user, RecommendationMode.DISH_BASED, dishName, limited.size());

        return limited;
    }

    public static String normalizeCuisine(String cuisine) {
        if (cuisine == null) return "";
        return cuisine
                .replaceAll("[\\uFEFF\\u200B-\\u200D\\r\\n]", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+recipes?\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean matchesCuisine(String recipeCuisine, List<String> preferredCuisines) {
        if (preferredCuisines == null || preferredCuisines.isEmpty()) {
            return true; // No filter
        }
        if (recipeCuisine == null) {
            return false;
        }
        String normRc = normalizeCuisine(recipeCuisine);
        for (String pref : preferredCuisines) {
            if (pref == null || pref.isBlank() || pref.trim().equalsIgnoreCase("all")) {
                return true;
            }
            String normPref = normalizeCuisine(pref);

            if (normRc.equals(normPref)) {
                return true;
            }

            if ("indian".equals(normPref) && (normRc.contains("indian") || normRc.contains("tamil")
                    || normRc.contains("punjabi") || normRc.contains("bengali") || normRc.contains("kerala")
                    || normRc.contains("awadhi") || normRc.contains("chettinad") || normRc.contains("mughlai")
                    || normRc.contains("konkan") || normRc.contains("karnataka") || normRc.contains("andhra")
                    || normRc.contains("gujarati") || normRc.contains("rajasthani") || normRc.contains("goan")
                    || normRc.contains("kashmiri") || normRc.contains("mangalorean") || normRc.contains("parsi"))) {
                return true;
            }

            if ("asian".equals(normPref) && (normRc.contains("asian") || normRc.contains("chinese")
                    || normRc.contains("japanese") || normRc.contains("thai") || normRc.contains("korean")
                    || normRc.contains("vietnamese") || normRc.contains("malaysian") || normRc.contains("indo chinese"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDietaryConflict(DietaryType recipeDiet, DietaryType userDiet) {
        if (userDiet == null) {
            return false;
        }
        return !recommendationEngine.isDietaryCompatible(recipeDiet, userDiet);
    }

    private boolean hasAllergyConflict(Recipe recipe, List<String> allergies) {
        if (allergies == null || allergies.isEmpty() || recipe.getRecipeIngredients() == null) {
            return false;
        }

        for (var ri : recipe.getRecipeIngredients()) {
            if (ri.getIngredient() == null) continue;
            String ingName = ri.getIngredient().getName() != null ? ri.getIngredient().getName().toLowerCase() : "";
            String category = ri.getIngredient().getCategory() != null ? ri.getIngredient().getCategory().toLowerCase() : "";

            for (String allergy : allergies) {
                if (allergy == null || allergy.isBlank()) continue;
                String cleanAllergy = allergy.trim().toLowerCase();

                // Direct name match or category match
                if (ingName.contains(cleanAllergy) || cleanAllergy.contains(ingName)
                        || category.contains(cleanAllergy) || cleanAllergy.contains(category)) {
                    return true;
                }

                // Check common allergy aliases
                if (isAllergyMatch(cleanAllergy, ingName, category)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAllergyMatch(String allergy, String ingName, String category) {
        if ("dairy".equals(allergy)) {
            return category.contains("dairy") || ingName.matches(".*\\b(milk|butter|cheese|mozzarella|parmesan|paneer|cream|yogurt)\\b.*");
        }
        if ("nut".equals(allergy) || "nuts".equals(allergy) || "peanut".equals(allergy) || "peanuts".equals(allergy)) {
            return ingName.matches(".*\\b(peanut|peanuts|almond|almonds|cashew|cashews|walnut|walnuts|pecan|pecans)\\b.*");
        }
        if ("gluten".equals(allergy)) {
            return category.contains("grains") || ingName.matches(".*\\b(wheat|flour|pasta|dough|bread|crouton|croutons)\\b.*");
        }
        if ("egg".equals(allergy) || "eggs".equals(allergy)) {
            return ingName.matches(".*\\b(egg|eggs|mayonnaise)\\b.*");
        }
        if ("shellfish".equals(allergy) || "seafood".equals(allergy) || "fish".equals(allergy)) {
            return ingName.matches(".*\\b(shrimp|prawn|crab|lobster|oyster|clam|mussel|fish|salmon|tuna)\\b.*");
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

    private RecipeDetailResponse mapRecipeForEvaluation(Recipe recipe) {
        List<RecipeIngredientResponse> ingredients = recipe.getRecipeIngredients() != null
                ? recipe.getRecipeIngredients().stream().map(ri -> RecipeIngredientResponse.builder()
                        .id(ri.getId())
                        .ingredientId(ri.getIngredient().getId())
                        .ingredientName(ri.getIngredient().getName())
                        .category(ri.getIngredient().getCategory())
                        .quantity(ri.getQuantity())
                        .unit(ri.getUnit())
                        .isOptional(ri.getIsOptional())
                        .notes(ri.getNotes())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return RecipeDetailResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .cuisine(recipe.getCuisine())
                .mealType(recipe.getMealType())
                .dietaryType(recipe.getDietaryType())
                .difficulty(recipe.getDifficulty())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .totalTimeMinutes(recipe.getTotalTimeMinutes())
                .servings(recipe.getServings())
                .calories(recipe.getCalories())
                .spiceLevel(recipe.getSpiceLevel())
                .instructions(recipe.getInstructions())
                .imageUrl(recipe.getImageUrl())
                .videoUrl(recipe.getVideoUrl())
                .sourceUrl(recipe.getSourceUrl())
                .ingredients(ingredients)
                .build();
    }

    private List<Recipe> cachedPublishedRecipes = null;
    private long lastCacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 3600_000L; // 1 hour in-memory cache

    public synchronized List<Recipe> getAllPublishedRecipes() {
        long now = System.currentTimeMillis();
        if (cachedPublishedRecipes == null || (now - lastCacheTimestamp > CACHE_DURATION_MS)) {
            cachedPublishedRecipes = recipeRepository.findAllPublishedWithIngredients();
            lastCacheTimestamp = now;
        }
        return cachedPublishedRecipes;
    }

    public synchronized void evictCache() {
        cachedPublishedRecipes = null;
        lastCacheTimestamp = 0;
    }
}
