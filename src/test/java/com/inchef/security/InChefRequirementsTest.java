package com.inchef.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import com.inchef.common.MeasurementUnit;
import com.inchef.exception.ConflictException;
import com.inchef.favorite.FavoriteRepository;
import com.inchef.favorite.FavoriteService;
import com.inchef.ingredient.Ingredient;
import com.inchef.ingredient.IngredientRepository;
import com.inchef.pantry.UserIngredient;
import com.inchef.pantry.UserIngredientRepository;
import com.inchef.pantry.UserIngredientRequest;
import com.inchef.pantry.UserIngredientService;
import com.inchef.preference.DietaryType;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeCreateRequest;
import com.inchef.recipe.RecipeDetailResponse;
import com.inchef.recipe.RecipeIngredient;
import com.inchef.recipe.RecipeIngredientRepository;
import com.inchef.recipe.RecipeIngredientRequest;
import com.inchef.recipe.RecipeRepository;
import com.inchef.recipe.RecipeService;
import com.inchef.recipe.RecipeSummaryResponse;
import com.inchef.recommendation.IngredientRecommendationRequest;
import com.inchef.recommendation.RecommendationEngine;
import com.inchef.recommendation.RecommendationResponse;
import com.inchef.recommendation.RecommendationService;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class InChefRequirementsTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeIngredientRepository recipeIngredientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIngredientRepository userIngredientRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserIngredientService userIngredientService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private RecommendationEngine recommendationEngine;

    private User testUser;
    private Recipe rBiryani, rPaneer, rDosa, rPeanutChutney, rGreenPea, rEggCurry, rEggplant;

    @BeforeEach
    void setUpTestData() {
        recommendationService.evictCache();
        favoriteRepository.deleteAll();
        userIngredientRepository.deleteAll();
        recipeIngredientRepository.deleteAll();
        recipeRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .name("Test Chef")
                .email("testchef" + System.nanoTime() + "@inchef.com")
                .build());

        // Canonical ingredients
        Ingredient chicken = createIng("Chicken", "Proteins");
        Ingredient basmatiRice = createIng("Basmati Rice", "Grains");
        Ingredient onion = createIng("Onion", "Vegetables");
        Ingredient garlic = createIng("Garlic", "Vegetables");
        Ingredient ginger = createIng("Ginger", "Vegetables");

        Ingredient paneer = createIng("Paneer", "Dairy & Eggs");
        Ingredient tomato = createIng("Tomato", "Vegetables");
        Ingredient butter = createIng("Butter", "Dairy & Eggs");
        Ingredient cream = createIng("Cream", "Dairy & Eggs");

        Ingredient dosaBatter = createIng("Dosa Batter", "Grains");
        Ingredient potato = createIng("Potato", "Vegetables");
        Ingredient mustard = createIng("Mustard", "Spices & Herbs");

        Ingredient peanut = createIng("Peanut", "Proteins");
        Ingredient greenChilli = createIng("Green Chilli", "Vegetables");
        Ingredient salt = createIng("Salt", "Spices & Herbs");

        Ingredient pea = createIng("Pea", "Vegetables");
        Ingredient egg = createIng("Egg", "Proteins");
        Ingredient eggplant = createIng("Eggplant", "Vegetables");

        // 1. Chicken Biryani (4 ingredients, Non-Veg, 50 mins, Indian, Dinner)
        rBiryani = saveRecipe("Chicken Biryani", "Authentic Chicken Biryani", "Biryani", "Indian",
                MealType.DINNER, DietaryType.NON_VEGETARIAN, 50,
                List.of(link(chicken), link(basmatiRice), link(onion), link(garlic)));

        // 2. Paneer Butter Masala (4 ingredients, Vegetarian, 35 mins, North Indian Recipes, Lunch)
        rPaneer = saveRecipe("Paneer Butter Masala", "Paneer Butter Masala", "Creamy paneer curry", "North Indian Recipes",
                MealType.LUNCH, DietaryType.VEGETARIAN, 35,
                List.of(link(paneer), link(tomato), link(butter), link(cream)));

        // 3. Masala Dosa (3 ingredients, Vegetarian, 20 mins, South Indian Recipes, Breakfast)
        rDosa = saveRecipe("Masala Dosa", "Crispy Masala Dosa", "Crispy crepe with potato filling", "South Indian Recipes",
                MealType.BREAKFAST, DietaryType.VEGETARIAN, 20,
                List.of(link(dosaBatter), link(potato), link(onion)));

        // 4. Peanut Chutney (4 ingredients: Peanut, Green Chilli, Garlic, Salt, 10 mins, South Indian Recipes)
        rPeanutChutney = saveRecipe("Peanut Chutney", "South Indian Peanut Chutney", "Spicy nutty dip", "South Indian Recipes",
                MealType.SNACK, DietaryType.VEGETARIAN, 10,
                List.of(link(peanut), link(greenChilli), link(garlic), link(salt)));

        // 5. Green Pea Curry (4 ingredients: Pea, Tomato, Onion, Salt, 20 mins, North Indian Recipes)
        rGreenPea = saveRecipe("Green Pea Curry", "Matar Masala", "Fresh green pea curry", "North Indian Recipes",
                MealType.LUNCH, DietaryType.VEGETARIAN, 20,
                List.of(link(pea), link(tomato), link(onion), link(salt)));

        // 6. Egg Curry (4 ingredients: Egg, Tomato, Onion, Garlic, 25 mins, Indian, Dinner)
        rEggCurry = saveRecipe("Egg Curry", "Spicy Egg Curry", "Boiled egg curry", "Indian",
                MealType.DINNER, DietaryType.EGGETARIAN, 25,
                List.of(link(egg), link(tomato), link(onion), link(garlic)));

        // 7. Baingan Bharta (Eggplant) (4 ingredients: Eggplant, Tomato, Onion, Garlic, 35 mins, North Indian Recipes)
        rEggplant = saveRecipe("Baingan Bharta (Eggplant)", "Roasted Eggplant Mash", "Smoky mashed eggplant", "North Indian Recipes",
                MealType.DINNER, DietaryType.VEGETARIAN, 35,
                List.of(link(eggplant), link(tomato), link(onion), link(garlic)));

        recommendationService.evictCache();
    }

    private Ingredient createIng(String name, String cat) {
        return ingredientRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> ingredientRepository.save(
                        Ingredient.builder().name(name).category(cat).defaultUnit(MeasurementUnit.PIECES).build()
                ));
    }

    private RecipeIngredient link(Ingredient ing) {
        return RecipeIngredient.builder().ingredient(ing).quantity(1.0).unit(MeasurementUnit.PIECES).build();
    }

    private Recipe saveRecipe(String title, String transName, String desc, String cuisine, MealType meal, DietaryType diet, int time, List<RecipeIngredient> links) {
        Recipe r = Recipe.builder()
                .title(title)
                .translatedRecipeName(transName)
                .description(desc)
                .cuisine(cuisine)
                .mealType(meal)
                .dietaryType(diet)
                .difficulty(DifficultyLevel.MEDIUM)
                .prepTimeMinutes(10)
                .cookTimeMinutes(time - 10)
                .totalTimeMinutes(time)
                .servings(4)
                .instructions("Cook thoroughly and serve hot.")
                .ingredientText(title + " ingredients")
                .sourceUrl("https://example.com/" + title.toLowerCase().replace(" ", "-"))
                .sourceDataset("IndianFoodDataset")
                .sourceRecipeId("REC-" + System.nanoTime())
                .isPublished(true)
                .build();
        Recipe saved = recipeRepository.save(r);
        for (RecipeIngredient ri : links) {
            ri.setRecipe(saved);
            saved.addRecipeIngredient(ri);
            recipeIngredientRepository.save(ri);
        }
        return saved;
    }

    // =========================================================================
    // 1. Direct Recipe Search Tests (Workflow A)
    // =========================================================================
    @Nested
    @DisplayName("Workflow A: Direct Recipe Search")
    class DirectSearchTests {

        @Test
        @DisplayName("Direct search for 'Chicken Biryani' returns matching recipe")
        void testSearchChickenBiryani() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    "Chicken Biryani", null, null, null, null, null, PageRequest.of(0, 10), null);
            assertFalse(page.isEmpty());
            assertTrue(page.getContent().stream().anyMatch(r -> r.getTitle().contains("Chicken Biryani")));
        }

        @Test
        @DisplayName("Direct search for 'Paneer Butter Masala' returns matching recipe")
        void testSearchPaneerButterMasala() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    "Paneer Butter Masala", null, null, null, null, null, PageRequest.of(0, 10), null);
            assertFalse(page.isEmpty());
            assertTrue(page.getContent().stream().anyMatch(r -> r.getTitle().contains("Paneer Butter Masala")));
        }

        @Test
        @DisplayName("Direct search for 'Masala Dosa' returns matching recipe")
        void testSearchMasalaDosa() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    "Masala Dosa", null, null, null, null, null, PageRequest.of(0, 10), null);
            assertFalse(page.isEmpty());
            assertTrue(page.getContent().stream().anyMatch(r -> r.getTitle().contains("Masala Dosa")));
        }

        @Test
        @DisplayName("Case-insensitive direct search works across variations")
        void testCaseInsensitiveSearch() {
            Page<RecipeSummaryResponse> pageLower = recipeService.searchRecipes(
                    "chicken biryani", null, null, null, null, null, PageRequest.of(0, 10), null);
            Page<RecipeSummaryResponse> pageUpper = recipeService.searchRecipes(
                    "CHICKEN BIRYANI", null, null, null, null, null, PageRequest.of(0, 10), null);
            Page<RecipeSummaryResponse> pageMixed = recipeService.searchRecipes(
                    "ChIcKeN bIrYaNi", null, null, null, null, null, PageRequest.of(0, 10), null);

            assertEquals(pageLower.getTotalElements(), pageUpper.getTotalElements());
            assertEquals(pageLower.getTotalElements(), pageMixed.getTotalElements());
            assertFalse(pageLower.isEmpty());
        }

        @Test
        @DisplayName("Partial title match returns recipe")
        void testPartialTitleSearch() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    "Biryani", null, null, null, null, null, PageRequest.of(0, 10), null);
            assertFalse(page.isEmpty());
            assertEquals("Chicken Biryani", page.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("No-result search returns empty page cleanly without exception")
        void testNoResultSearch() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    "NonExistentDish123456", null, null, null, null, null, PageRequest.of(0, 10), null);
            assertTrue(page.isEmpty());
            assertEquals(0, page.getTotalElements());
        }

        @Test
        @DisplayName("Search with 'all' filter does not filter out valid recipes")
        void testSearchFilterAllValueIgnored() {
            Page<RecipeSummaryResponse> page = recipeService.searchRecipes(
                    null, "all", null, null, null, null, PageRequest.of(0, 20), null);
            assertEquals(7, page.getTotalElements());
        }
    }

    // =========================================================================
    // 2. Ingredient-Based Recommendation Tests (Workflow B)
    // =========================================================================
    @Nested
    @DisplayName("Workflow B: Ingredient-Based Recommendation")
    class RecommendationTests {

        @Test
        @DisplayName("Exact match: 100% ingredients available returns top score")
        void testExactIngredientMatch() {
            // Peanut Chutney has 4 ingredients: Peanut, Green Chilli, Garlic, Salt
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Peanut", "Green Chilli", "Garlic", "Salt"))
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertFalse(recs.isEmpty());
            RecommendationResponse top = recs.get(0);
            assertEquals(rPeanutChutney.getId(), top.getRecipe().getId());
            assertEquals(100.0, top.getIngredientMatchPercentage());
            assertEquals(4, top.getMatchedIngredients().size());
            assertEquals(0, top.getMissingIngredients().size());
            assertEquals(100, top.getMatchScore());
        }

        @Test
        @DisplayName("Boundary behavior at 75% threshold: 3 of 4 ingredients = 75% -> ELIGIBLE")
        void testThresholdBoundaryEligibility() {
            // User enters 3 of 4 ingredients for Chicken Biryani (Chicken, Basmati Rice, Onion)
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Chicken", "Basmati Rice", "Onion"))
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertTrue(recs.stream().anyMatch(r -> r.getRecipe().getId().equals(rBiryani.getId())),
                    "Recipe with 75% ingredient match must be eligible at 75% threshold");

            RecommendationResponse biryaniRec = recs.stream()
                    .filter(r -> r.getRecipe().getId().equals(rBiryani.getId()))
                    .findFirst().orElseThrow();
            assertEquals(75.0, biryaniRec.getIngredientMatchPercentage());
            assertEquals(3, biryaniRec.getMatchedIngredients().size());
            assertEquals(1, biryaniRec.getMissingIngredients().size());
        }

        @Test
        @DisplayName("Exclusion below 75% threshold: 2 of 4 ingredients = 50% -> EXCLUDED")
        void testThresholdExclusion() {
            // User enters only 2 of 4 ingredients for Paneer Butter Masala (Paneer, Tomato)
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Paneer", "Tomato"))
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertFalse(recs.stream().anyMatch(r -> r.getRecipe().getId().equals(rPaneer.getId())),
                    "Recipe with 50% match must be excluded by 75% threshold");
        }

        @Test
        @DisplayName("Duplicate input ingredients are deduplicated and do not inflate scores")
        void testDuplicateInputIngredients() {
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Chicken", "chicken", "CHICKEN", "Basmati Rice", "Onion"))
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            RecommendationResponse biryaniRec = recs.stream()
                    .filter(r -> r.getRecipe().getId().equals(rBiryani.getId()))
                    .findFirst().orElseThrow();
            // Still 3 matched ingredients out of 4, coverage is 75%
            assertEquals(75.0, biryaniRec.getIngredientMatchPercentage());
            assertEquals(3, biryaniRec.getMatchedIngredients().size());
        }

        @Test
        @DisplayName("Unknown ingredients return empty results cleanly")
        void testUnknownIngredientsReturnEmpty() {
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("UnobtainiumFlour", "KryptoniteSpice"))
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertTrue(recs.isEmpty());
        }

        @Test
        @DisplayName("False-positive prevention: 'pea' must NOT match 'peanut'")
        void testPeaDoesNotMatchPeanut() {
            assertFalse(recommendationEngine.isIngredientMatched("Peanut", java.util.Set.of("pea")));
            assertFalse(recommendationEngine.isIngredientMatched("Peanut", java.util.Set.of("peas")));
            assertFalse(recommendationEngine.isIngredientMatched("Pea", java.util.Set.of("peanut")));
            assertFalse(recommendationEngine.isIngredientMatched("Pea", java.util.Set.of("peanuts")));
        }

        @Test
        @DisplayName("False-positive prevention: 'egg' must NOT match 'eggplant'")
        void testEggDoesNotMatchEggplant() {
            assertFalse(recommendationEngine.isIngredientMatched("Eggplant", java.util.Set.of("egg")));
            assertFalse(recommendationEngine.isIngredientMatched("Eggplant", java.util.Set.of("eggs")));
            assertFalse(recommendationEngine.isIngredientMatched("Egg", java.util.Set.of("eggplant")));
            assertFalse(recommendationEngine.isIngredientMatched("Egg", java.util.Set.of("eggplants")));
        }

        @Test
        @DisplayName("Dietary filter excludes non-compliant recipes")
        void testDietaryFilterExclusion() {
            // User enters common ingredients: Tomato, Onion, Garlic (matches Egg Curry, Green Pea Curry, Baingan Bharta)
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Tomato", "Onion", "Garlic", "Egg"))
                    .dietaryType(DietaryType.VEGETARIAN) // strictly vegetarian
                    .minIngredientMatch(50.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            // Non-veg and eggetarian recipes must be excluded
            assertFalse(recs.stream().anyMatch(r -> r.getRecipe().getId().equals(rEggCurry.getId())),
                    "Vegetarian filter must exclude eggetarian Egg Curry");
            assertFalse(recs.stream().anyMatch(r -> r.getRecipe().getId().equals(rBiryani.getId())),
                    "Vegetarian filter must exclude non-vegetarian Biryani");
        }

        @Test
        @DisplayName("Cuisine filter excludes non-matching cuisines")
        void testCuisineFilterExclusion() {
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Tomato", "Onion", "Garlic", "Pea", "Salt"))
                    .cuisine("South Indian Recipes")
                    .minIngredientMatch(50.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            for (RecommendationResponse r : recs) {
                assertTrue(r.getRecipe().getCuisine().contains("South Indian"),
                        "Expected only South Indian recipes but found: " + r.getRecipe().getCuisine());
            }
        }

        @Test
        @DisplayName("Maximum cooking time filter excludes longer recipes")
        void testMaxCookingTimeFilter() {
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Chicken", "Basmati Rice", "Onion", "Garlic"))
                    .maxCookingTime(30) // Biryani takes 50 mins
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertFalse(recs.stream().anyMatch(r -> r.getRecipe().getId().equals(rBiryani.getId())),
                    "Biryani takes 50 mins and must be excluded when max time is 30 mins");
        }

        @Test
        @DisplayName("Pantry-derived recommendations succeed when usePantry=true")
        void testPantryDerivedRecommendations() {
            // Add ingredients to user pantry
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Peanut", "Proteins", 2.0, MeasurementUnit.CUPS, null));
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Green Chilli", "Vegetables", 4.0, MeasurementUnit.PIECES, null));
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Garlic", "Vegetables", 5.0, MeasurementUnit.PIECES, null));
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Salt", "Spices & Herbs", 1.0, MeasurementUnit.PINCH, null));

            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .userId(testUser.getId())
                    .usePantry(true)
                    .minIngredientMatch(75.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            assertFalse(recs.isEmpty());
            assertEquals(rPeanutChutney.getId(), recs.get(0).getRecipe().getId());
            assertEquals(100.0, recs.get(0).getIngredientMatchPercentage());
        }

        @Test
        @DisplayName("Deterministic ranking orders recipes by match score desc, cooking time asc, id asc")
        void testDeterministicRankingOrder() {
            IngredientRecommendationRequest req = IngredientRecommendationRequest.builder()
                    .ingredients(List.of("Tomato", "Onion", "Garlic", "Salt"))
                    .minIngredientMatch(50.0)
                    .build();

            List<RecommendationResponse> recs = recommendationService.recommendByIngredients(req);
            for (int i = 0; i < recs.size() - 1; i++) {
                RecommendationResponse curr = recs.get(i);
                RecommendationResponse next = recs.get(i + 1);
                assertTrue(curr.getMatchScore() >= next.getMatchScore());
            }
        }
    }

    // =========================================================================
    // 3. Database Integrity & Model Tests
    // =========================================================================
    @Nested
    @DisplayName("Database Integrity & Model Guarantees")
    class DatabaseIntegrityTests {

        @Test
        @DisplayName("Favorites uniqueness prevents duplicate saves for same user and recipe")
        void testFavoriteUniqueness() {
            favoriteService.addFavorite(testUser.getId(), rBiryani.getId());

            assertThrows(ConflictException.class, () ->
                    favoriteService.addFavorite(testUser.getId(), rBiryani.getId()));
        }

        @Test
        @DisplayName("Pantry uniqueness updates quantity rather than creating duplicate row")
        void testPantryUniqueness() {
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Turmeric", "Spices", 1.0, MeasurementUnit.TEASPOONS, null));
            assertEquals(1, userIngredientRepository.findByUserId(testUser.getId()).size());

            // Add again with updated quantity
            userIngredientService.addOrUpdateUserIngredient(testUser.getId(),
                    new UserIngredientRequest(null, "Turmeric", "Spices", 2.5, MeasurementUnit.TEASPOONS, null));
            assertEquals(1, userIngredientRepository.findByUserId(testUser.getId()).size(),
                    "Pantry should not contain duplicate row for same user and ingredient");

            UserIngredient item = userIngredientRepository.findByUserId(testUser.getId()).get(0);
            assertEquals(2.5, item.getQuantity());
        }

        @Test
        @DisplayName("Recipe details retrieval returns full fields including translated names and ingredients")
        void testRecipeDetailsRetrieval() {
            RecipeDetailResponse detail = recipeService.getRecipeById(rBiryani.getId(), testUser.getId());
            assertNotNull(detail);
            assertEquals("Chicken Biryani", detail.getTitle());
            assertEquals("Authentic Chicken Biryani", detail.getTranslatedRecipeName());
            assertNotNull(detail.getIngredients());
            assertFalse(detail.getIngredients().isEmpty());
            assertEquals(50, detail.getTotalTimeMinutes());
            assertEquals("Indian", detail.getCuisine());
            assertEquals(DietaryType.NON_VEGETARIAN, detail.getDietaryType());
            assertEquals(MealType.DINNER, detail.getMealType());
            assertNotNull(detail.getSourceUrl());
        }
    }
}
