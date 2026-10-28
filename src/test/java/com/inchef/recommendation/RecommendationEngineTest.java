package com.inchef.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

import com.inchef.preference.DietaryType;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeDetailResponse;
import com.inchef.recipe.RecipeIngredientResponse;

class RecommendationEngineTest {

    private RecommendationEngine recommendationEngine;

    @BeforeEach
    void setUp() {
        recommendationEngine = new RecommendationEngine();
    }

    @Test
    @DisplayName("Should score 100% when all ingredients, cuisine, diet, time, and meal match perfectly")
    void testPerfectMatch() {
        Recipe recipe = Recipe.builder()
                .id(1L)
                .title("Tomato Basil Pasta")
                .cuisine("Italian")
                .dietaryType(DietaryType.VEGETARIAN)
                .mealType(MealType.DINNER)
                .totalTimeMinutes(25)
                .build();

        RecipeDetailResponse detail = RecipeDetailResponse.builder()
                .id(1L)
                .title("Tomato Basil Pasta")
                .ingredients(Arrays.asList(
                        RecipeIngredientResponse.builder().ingredientName("Pasta").build(),
                        RecipeIngredientResponse.builder().ingredientName("Tomato").build(),
                        RecipeIngredientResponse.builder().ingredientName("Basil").build()
                ))
                .build();

        Set<String> userIngredients = new HashSet<>(Arrays.asList("pasta", "tomato", "basil", "olive oil"));

        RecommendationEngine.PreferenceContext prefs = new RecommendationEngine.PreferenceContext();
        prefs.dietaryType = DietaryType.VEGETARIAN;
        prefs.preferredCuisines = List.of("Italian");
        prefs.maxCookingTime = 30;
        prefs.mealType = MealType.DINNER;

        RecommendationResponse result = recommendationEngine.evaluateRecipe(recipe, userIngredients, prefs, detail);

        assertNotNull(result);
        assertEquals(100, result.getMatchScore());
        assertEquals(3, result.getMatchedIngredients().size());
        assertEquals(0, result.getMissingIngredients().size());
        assertEquals(60.0, result.getScoreBreakdown().getIngredientScore());
        assertEquals(15.0, result.getScoreBreakdown().getDietaryScore());
        assertEquals(10.0, result.getScoreBreakdown().getCuisineScore());
        assertEquals(10.0, result.getScoreBreakdown().getCookingTimeScore());
        assertEquals(5.0, result.getScoreBreakdown().getCategoryScore());
    }

    @Test
    @DisplayName("Should accurately report missing ingredients and proportional ingredient score")
    void testPartialMatchMissingIngredients() {
        Recipe recipe = Recipe.builder()
                .id(2L)
                .title("Chicken Stir Fry")
                .cuisine("Asian")
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .mealType(MealType.DINNER)
                .totalTimeMinutes(20)
                .build();

        RecipeDetailResponse detail = RecipeDetailResponse.builder()
                .id(2L)
                .title("Chicken Stir Fry")
                .ingredients(Arrays.asList(
                        RecipeIngredientResponse.builder().ingredientName("Chicken").build(),
                        RecipeIngredientResponse.builder().ingredientName("Broccoli").build(),
                        RecipeIngredientResponse.builder().ingredientName("Soy Sauce").build(),
                        RecipeIngredientResponse.builder().ingredientName("Garlic").build()
                ))
                .build();

        // User has only Chicken and Garlic (2 of 4 matched)
        Set<String> userIngredients = new HashSet<>(Arrays.asList("chicken", "garlic"));

        RecommendationEngine.PreferenceContext prefs = new RecommendationEngine.PreferenceContext();
        prefs.dietaryType = DietaryType.NON_VEGETARIAN;
        prefs.preferredCuisines = List.of("Asian");
        prefs.maxCookingTime = 30;
        prefs.mealType = MealType.DINNER;

        RecommendationResponse result = recommendationEngine.evaluateRecipe(recipe, userIngredients, prefs, detail);

        assertNotNull(result);
        assertEquals(2, result.getMatchedIngredients().size());
        assertTrue(result.getMatchedIngredients().contains("Chicken"));
        assertTrue(result.getMatchedIngredients().contains("Garlic"));

        assertEquals(2, result.getMissingIngredients().size());
        assertTrue(result.getMissingIngredients().contains("Broccoli"));
        assertTrue(result.getMissingIngredients().contains("Soy Sauce"));

        // 2/4 = 50% * 60 = 30 points for ingredients
        assertEquals(30.0, result.getScoreBreakdown().getIngredientScore());
    }

    @Test
    @DisplayName("Should award 0 dietary score when vegetarian user encounters non-vegetarian recipe")
    void testDietaryMismatch() {
        Recipe recipe = Recipe.builder()
                .id(3L)
                .title("Beef Steak")
                .cuisine("American")
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .totalTimeMinutes(25)
                .build();

        RecipeDetailResponse detail = RecipeDetailResponse.builder()
                .id(3L)
                .ingredients(List.of(RecipeIngredientResponse.builder().ingredientName("Beef").build()))
                .build();

        Set<String> userIngredients = new HashSet<>(List.of("beef"));

        RecommendationEngine.PreferenceContext prefs = new RecommendationEngine.PreferenceContext();
        prefs.dietaryType = DietaryType.VEGETARIAN; // User is strictly vegetarian

        RecommendationResponse result = recommendationEngine.evaluateRecipe(recipe, userIngredients, prefs, detail);

        assertEquals(0.0, result.getScoreBreakdown().getDietaryScore());
    }

    @Test
    @DisplayName("Should prevent false-positive substring matches while supporting token and plural normalization")
    void testFalsePositivePreventionAndNormalization() {
        // "pea" should NOT match "peanuts"
        assertFalse(recommendationEngine.isIngredientMatched("Peanuts", Set.of("pea")));
        // "ice" should NOT match "rice"
        assertFalse(recommendationEngine.isIngredientMatched("Rice", Set.of("ice")));
        // "corn" should NOT match "peppercorn"
        assertFalse(recommendationEngine.isIngredientMatched("Peppercorn", Set.of("corn")));
        // "egg" should NOT match "eggplant"
        assertFalse(recommendationEngine.isIngredientMatched("Eggplant", Set.of("egg")));
        // "sesame oil" should NOT match "olive oil"
        assertFalse(recommendationEngine.isIngredientMatched("Sesame Oil", Set.of("olive oil")));

        // Valid matches:
        // Plural to singular
        assertTrue(recommendationEngine.isIngredientMatched("Tomatoes", Set.of("tomato")));
        assertTrue(recommendationEngine.isIngredientMatched("Tomato", Set.of("tomatoes")));
        assertTrue(recommendationEngine.isIngredientMatched("Onions", Set.of("onion")));
        assertTrue(recommendationEngine.isIngredientMatched("Eggs", Set.of("egg")));

        // Token match: chicken matches chicken breast
        assertTrue(recommendationEngine.isIngredientMatched("Chicken Breast", Set.of("chicken")));
        assertTrue(recommendationEngine.isIngredientMatched("Chicken", Set.of("chicken breast")));

        // Multi-word token match: onion matches green onions
        assertTrue(recommendationEngine.isIngredientMatched("Green Onions", Set.of("onion")));
    }

    @Test
    @DisplayName("Should evaluate dietary compatibility correctly")
    void testDietaryCompatibility() {
        // Non-veg user accepts all
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.NON_VEGETARIAN, DietaryType.NON_VEGETARIAN));
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.VEGETARIAN, DietaryType.NON_VEGETARIAN));
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.VEGAN, DietaryType.NON_VEGETARIAN));

        // Vegetarian user accepts Vegetarian and Vegan, rejects Non-Veg
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.VEGETARIAN, DietaryType.VEGETARIAN));
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.VEGAN, DietaryType.VEGETARIAN));
        assertFalse(recommendationEngine.isDietaryCompatible(DietaryType.NON_VEGETARIAN, DietaryType.VEGETARIAN));

        // Vegan user accepts Vegan, rejects Vegetarian and Non-Veg
        assertTrue(recommendationEngine.isDietaryCompatible(DietaryType.VEGAN, DietaryType.VEGAN));
        assertFalse(recommendationEngine.isDietaryCompatible(DietaryType.VEGETARIAN, DietaryType.VEGAN));
        assertFalse(recommendationEngine.isDietaryCompatible(DietaryType.NON_VEGETARIAN, DietaryType.VEGAN));
    }
}

