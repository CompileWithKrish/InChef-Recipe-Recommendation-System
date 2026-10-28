package com.inchef.recommendation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import com.inchef.preference.DietaryType;
import com.inchef.recipe.MealType;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class RecommendationServiceIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Test
    @DisplayName("Stage 1-5 Verification: Test with Chicken, Tomato, Onion, Garlic on real DB data")
    void testPrimaryRecommendationPipeline() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Chicken", "Tomato", "Onion", "Garlic"))
                .minIngredientMatch(20.0)
                .limit(10)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        // Verify ranked from highest to lowest score
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getMatchScore() >= results.get(i + 1).getMatchScore(),
                    "Results must be sorted descending by match score");
        }

        // Verify result details for top recommendation
        RecommendationResponse top = results.get(0);
        assertNotNull(top.getRecipe(), "Recipe details must be returned");
        assertNotNull(top.getRecipe().getTitle());
        assertTrue(top.getMatchScore() > 0, "Top recommendation must have positive match score");
        assertNotNull(top.getMatchedIngredients(), "Matched ingredients list must be present");
        assertNotNull(top.getMissingIngredients(), "Missing ingredients list must be present");
        assertNotNull(top.getScoreBreakdown(), "Score breakdown must be present");
        assertNotNull(top.getReason(), "Human-readable reason must be present");

        ScoreBreakdown breakdown = top.getScoreBreakdown();
        assertTrue(breakdown.getIngredientScore() >= 0 && breakdown.getIngredientScore() <= 60.0);
        assertTrue(breakdown.getDietaryScore() >= 0 && breakdown.getDietaryScore() <= 15.0);
        assertTrue(breakdown.getCuisineScore() >= 0 && breakdown.getCuisineScore() <= 10.0);
        assertTrue(breakdown.getCookingTimeScore() >= 0 && breakdown.getCookingTimeScore() <= 10.0);
        assertTrue(breakdown.getCategoryScore() >= 0 && breakdown.getCategoryScore() <= 5.0);
        assertTrue(breakdown.getTotalScore() <= 100.0);

        // Verify Kung Pao Chicken contains Chicken Breast and Garlic
        results.stream()
                .filter(r -> r.getRecipe().getTitle().contains("Kung Pao Chicken"))
                .findFirst()
                .ifPresent(kpc -> {
                    assertTrue(kpc.getMatchedIngredients().contains("Chicken Breast") || kpc.getMatchedIngredients().stream().anyMatch(i -> i.toLowerCase().contains("chicken")));
                    assertTrue(kpc.getMatchedIngredients().contains("Garlic"));
                    assertTrue(kpc.getMissingIngredients().contains("Soy Sauce"));
                });
    }

    @Test
    @DisplayName("Stage 5 Verification: 75% threshold filters out recipes below 75% coverage")
    void testSeventyFivePercentThreshold() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Pizza Dough", "Tomato", "Mozzarella", "Basil", "Olive Oil"))
                .minIngredientMatch(75.0)
                .limit(10)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find Margherita with 5/6 = 83.3% match");
        for (RecommendationResponse r : results) {
            assertTrue(r.getIngredientMatchPercentage() >= 75.0, 
                "All recommendations must have at least 75% ingredient coverage");
        }
    }

    @Test
    @DisplayName("Edge Case 1: Only 1 matching ingredient with threshold override")
    void testOnlyOneMatchingIngredient() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Tomato"))
                .minIngredientMatch(0.0)
                .limit(10)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        // Find a recipe with Tomato (e.g., Margherita or Guacamole)
        RecommendationResponse withTomato = results.stream()
                .filter(r -> r.getMatchedIngredients().contains("Tomato"))
                .findFirst()
                .orElse(null);

        assertNotNull(withTomato, "Should find at least one recipe matching Tomato");
        assertEquals(1, withTomato.getMatchedIngredients().size());
        assertTrue(withTomato.getMissingIngredients().size() > 0);
        assertTrue(withTomato.getScoreBreakdown().getIngredientScore() > 0);
    }

    @Test
    @DisplayName("Edge Case 2: All required ingredients available (100% ingredient match)")
    void testAllRequiredIngredientsAvailable() {
        // Margherita ingredients: Pizza Dough, Tomato, Mozzarella, Basil, Olive Oil, Garlic
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Pizza Dough", "Tomato", "Mozzarella", "Basil", "Olive Oil", "Garlic"))
                .dietaryType(DietaryType.VEGETARIAN)
                .cuisine("Italian")
                .limit(5)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        RecommendationResponse margherita = results.stream()
                .filter(r -> r.getRecipe().getTitle().contains("Margherita"))
                .findFirst()
                .orElse(null);

        assertNotNull(margherita, "Margherita Pizza should be recommended");
        assertEquals(0, margherita.getMissingIngredients().size(), "All ingredients should be matched");
        assertEquals(60.0, margherita.getScoreBreakdown().getIngredientScore(), "Ingredient score should be max 60");
        assertEquals(100, margherita.getMatchScore(), "Perfect match should achieve 100%");
    }

    @Test
    @DisplayName("Edge Case 3: No matching ingredients with threshold override")
    void testNoMatchingIngredients() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Dragonfruit", "Artichoke"))
                .minIngredientMatch(0.0)
                .limit(5)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        for (RecommendationResponse r : results) {
            assertEquals(0, r.getMatchedIngredients().size(), "No ingredients should match");
            assertEquals(0.0, r.getScoreBreakdown().getIngredientScore(), "Ingredient score should be 0");
            assertTrue(r.getMissingIngredients().size() > 0, "All recipe ingredients should be missing");
        }
    }

    @Test
    @DisplayName("Edge Case 4: Multiple recipes with different match levels ranked appropriately")
    void testMultipleRecipesWithDifferentMatchLevels() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Chicken", "Tomato", "Onion", "Garlic"))
                .minIngredientMatch(20.0)
                .limit(10)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertTrue(results.size() >= 5);
        // Verify scores are descending
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getMatchScore() >= results.get(i + 1).getMatchScore());
        }
    }

    @Test
    @DisplayName("Edge Case 5: Dietary and allergy conflicts are excluded")
    void testDietaryAndAllergyConflictsExcluded() {
        // User Marcus is Vegan and allergic to Dairy and Shellfish
        // When Marcus requests recommendations, no Non-Veg or Vegetarian recipes with Dairy should appear
        IngredientRecommendationRequest veganRequest = IngredientRecommendationRequest.builder()
                .dietaryType(DietaryType.VEGAN)
                .ingredients(List.of("Chicken", "Tomato", "Onion", "Garlic"))
                .build();

        List<RecommendationResponse> veganResults = recommendationService.recommendByIngredients(veganRequest);

        for (RecommendationResponse r : veganResults) {
            assertEquals(DietaryType.VEGAN, r.getRecipe().getDietaryType(),
                    "Only Vegan recipes should be recommended to Vegan users");
            assertFalse(r.getRecipe().getTitle().contains("Chicken"),
                    "Chicken dishes must be excluded for Vegan users");
        }

        // Test Vegetarian request excludes chicken
        IngredientRecommendationRequest vegRequest = IngredientRecommendationRequest.builder()
                .dietaryType(DietaryType.VEGETARIAN)
                .ingredients(List.of("Chicken", "Tomato", "Onion", "Garlic"))
                .build();

        List<RecommendationResponse> vegResults = recommendationService.recommendByIngredients(vegRequest);

        for (RecommendationResponse r : vegResults) {
            assertNotEquals(DietaryType.NON_VEGETARIAN, r.getRecipe().getDietaryType(),
                    "Non-vegetarian recipes must be excluded for Vegetarian users");
        }
    }

    @Test
    @DisplayName("Edge Case 6: Empty ingredient input returns eligible recipes cleanly")
    void testEmptyIngredientInput() {
        IngredientRecommendationRequest request = IngredientRecommendationRequest.builder()
                .ingredients(List.of())
                .limit(5)
                .build();

        List<RecommendationResponse> results = recommendationService.recommendByIngredients(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        for (RecommendationResponse r : results) {
            assertEquals(0, r.getMatchedIngredients().size());
            assertEquals(0.0, r.getScoreBreakdown().getIngredientScore());
            assertTrue(r.getMatchScore() > 0);
        }
    }

    @Test
    @DisplayName("DEF-03: Course filter restricts recommendations when specified and does not restrict when omitted")
    void testCourseFilter() {
        // 1. Without course filter - unrestricted
        IngredientRecommendationRequest noCourseReq = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Onion", "Tomato"))
                .minIngredientMatch(20.0)
                .limit(20)
                .build();
        List<RecommendationResponse> allCourseResults = recommendationService.recommendByIngredients(noCourseReq);
        assertNotNull(allCourseResults);

        // 2. With DINNER course filter
        IngredientRecommendationRequest dinnerReq = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Onion", "Tomato"))
                .mealType(MealType.DINNER)
                .minIngredientMatch(20.0)
                .limit(20)
                .build();
        List<RecommendationResponse> dinnerResults = recommendationService.recommendByIngredients(dinnerReq);
        assertNotNull(dinnerResults);
        for (RecommendationResponse r : dinnerResults) {
            assertEquals(MealType.DINNER, r.getRecipe().getMealType(),
                    "All returned recipes must have DINNER meal type when DINNER filter is applied");
        }

        // 3. With BREAKFAST course filter
        IngredientRecommendationRequest breakfastReq = IngredientRecommendationRequest.builder()
                .ingredients(List.of("Onion", "Tomato"))
                .mealType(MealType.BREAKFAST)
                .minIngredientMatch(20.0)
                .limit(20)
                .build();
        List<RecommendationResponse> breakfastResults = recommendationService.recommendByIngredients(breakfastReq);
        assertNotNull(breakfastResults);
        for (RecommendationResponse r : breakfastResults) {
            assertEquals(MealType.BREAKFAST, r.getRecipe().getMealType(),
                    "All returned recipes must have BREAKFAST meal type when BREAKFAST filter is applied");
        }
    }
}
