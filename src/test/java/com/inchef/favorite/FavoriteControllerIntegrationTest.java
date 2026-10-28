package com.inchef.favorite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.inchef.auth.AuthResponse;
import com.inchef.auth.AuthService;
import com.inchef.auth.RegisterRequest;
import com.inchef.preference.DietaryType;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;
import com.inchef.security.JwtTokenProvider;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class FavoriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long user1Id;
    private String user1Token;

    private Long user2Id;
    private String user2Token;

    private Recipe sampleRecipe;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();

        // User 1
        String email1 = "fav_user1_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        AuthResponse auth1 = authService.register(new RegisterRequest(email1, "Favorite User One", "Password123"), res1);
        user1Id = auth1.getId();
        user1Token = jwtTokenProvider.generateToken(user1Id, email1);

        // User 2
        String email2 = "fav_user2_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        AuthResponse auth2 = authService.register(new RegisterRequest(email2, "Favorite User Two", "Password123"), res2);
        user2Id = auth2.getId();
        user2Token = jwtTokenProvider.generateToken(user2Id, email2);

        // Recipe
        sampleRecipe = recipeRepository.save(Recipe.builder()
                .title("Hyderabadi Biryani")
                .translatedRecipeName("Authentic Hyderabadi Biryani")
                .description("Rich layered rice and meat delicacy")
                .cuisine("Hyderabadi")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .difficulty(DifficultyLevel.HARD)
                .prepTimeMinutes(30)
                .cookTimeMinutes(45)
                .totalTimeMinutes(75)
                .imageUrl("https://images.example.com/hyderabadi-biryani.jpg")
                .isPublished(true)
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/favorites returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/favorites/count"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/favorites/" + sampleRecipe.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/favorites/" + sampleRecipe.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cross-user access to /api/v1/users/{userId}/favorites returns 403 Forbidden")
    void testCrossUserAccessReturns403() throws Exception {
        // User 2 accessing User 1's favorites list
        mockMvc.perform(get("/api/v1/users/" + user1Id + "/favorites")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // User 2 accessing User 1's favorite count
        mockMvc.perform(get("/api/v1/users/" + user1Id + "/favorites/count")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // User 2 adding favorite under User 1's path
        mockMvc.perform(post("/api/v1/users/" + user1Id + "/favorites/" + sampleRecipe.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // User 2 deleting favorite under User 1's path
        mockMvc.perform(delete("/api/v1/users/" + user1Id + "/favorites/" + sampleRecipe.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("End-to-end favorite lifecycle: Add, List with full DTO fields, Count, Duplicate prevention, Remove while preserving Recipe")
    void testFavoriteLifecycleEndToEnd() throws Exception {
        Long recipeId = sampleRecipe.getId();

        // 1. Initial count should be 0
        mockMvc.perform(get("/api/v1/favorites/count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count", is(0)));

        // 2. Check favorite is false initially
        mockMvc.perform(get("/api/v1/favorites/" + recipeId + "/check")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite", is(false)));

        // 3. Add favorite
        mockMvc.perform(post("/api/v1/favorites/" + recipeId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipeId", is(recipeId.intValue())))
                .andExpect(jsonPath("$.data.title", is("Hyderabadi Biryani")))
                .andExpect(jsonPath("$.data.cuisine", is("Hyderabadi")))
                .andExpect(jsonPath("$.data.imageUrl", is("https://images.example.com/hyderabadi-biryani.jpg")))
                .andExpect(jsonPath("$.data.totalTimeMinutes", is(75)))
                .andExpect(jsonPath("$.data.isFavorite", is(true)));

        // 4. Duplicate add must return 409 Conflict
        mockMvc.perform(post("/api/v1/favorites/" + recipeId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict());

        // 5. Count should be 1
        mockMvc.perform(get("/api/v1/favorites/count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count", is(1)));

        // 6. Check favorite is true
        mockMvc.perform(get("/api/v1/favorites/" + recipeId + "/check")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite", is(true)));

        // 7. Get user favorites returns complete recipe fields
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].recipeId", is(recipeId.intValue())))
                .andExpect(jsonPath("$.data[0].title", is("Hyderabadi Biryani")))
                .andExpect(jsonPath("$.data[0].translatedRecipeName", is("Authentic Hyderabadi Biryani")))
                .andExpect(jsonPath("$.data[0].cuisine", is("Hyderabadi")))
                .andExpect(jsonPath("$.data[0].imageUrl", is("https://images.example.com/hyderabadi-biryani.jpg")))
                .andExpect(jsonPath("$.data[0].totalTimeMinutes", is(75)))
                .andExpect(jsonPath("$.data[0].recipe.title", is("Hyderabadi Biryani"))); // nested backwards-compat

        // 8. User 2 favorites list is still empty (isolation)
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 9. Remove favorite
        mockMvc.perform(delete("/api/v1/favorites/" + recipeId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("removed")));

        // 10. Count is back to 0
        mockMvc.perform(get("/api/v1/favorites/count")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count", is(0)));

        // 11. Check favorite is false
        mockMvc.perform(get("/api/v1/favorites/" + recipeId + "/check")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite", is(false)));

        // 12. CRITICAL: The recipe itself MUST still exist in database!
        assertTrue(recipeRepository.existsById(recipeId), "Recipe entity must not be deleted when favorite is removed");
    }
}
