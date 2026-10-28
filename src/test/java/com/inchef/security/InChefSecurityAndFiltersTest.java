package com.inchef.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.inchef.auth.AuthResponse;
import com.inchef.auth.AuthService;
import com.inchef.auth.RegisterRequest;
import com.inchef.preference.DietaryType;
import com.inchef.recommendation.RecommendationService;
import com.inchef.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class InChefSecurityAndFiltersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RecommendationService recommendationService;

    private Long user1Id;
    private String user1Token;

    private Long user2Id;
    private String user2Token;

    @BeforeEach
    void setUp() {
        // Register test user 1
        String email1 = "test_sec_user1_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        AuthResponse auth1 = authService.register(new RegisterRequest(email1, "User One", "Pass1234"), res1);
        user1Id = auth1.getId();
        user1Token = jwtTokenProvider.generateToken(user1Id, email1);

        // Register test user 2
        String email2 = "test_sec_user2_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        AuthResponse auth2 = authService.register(new RegisterRequest(email2, "User Two", "Pass1234"), res2);
        user2Id = auth2.getId();
        user2Token = jwtTokenProvider.generateToken(user2Id, email2);
    }

    @Nested
    @DisplayName("1. Security: Anonymous, Cross-User, and Owner Access Tests")
    class SecurityTests {

        @Test
        @DisplayName("Anonymous access to pantry endpoints is blocked with HTTP 401")
        void testAnonymousPantryAccessBlocked() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/pantry"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/v1/users/" + user1Id + "/pantry")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ingredientId\":1,\"quantity\":2.0,\"unit\":\"PIECE\"}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/v1/users/" + user1Id + "/pantry/1"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/v1/users/" + user1Id + "/pantry"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous access to favorites endpoints is blocked with HTTP 401")
        void testAnonymousFavoritesAccessBlocked() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/favorites"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/v1/users/" + user1Id + "/favorites/10"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/v1/users/" + user1Id + "/favorites/10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous access to preferences endpoints is blocked with HTTP 401")
        void testAnonymousPreferencesAccessBlocked() throws Exception {
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/preferences"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"dietaryType\":\"Vegetarian\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Cross-user access is blocked with HTTP 403 Forbidden")
        void testCrossUserAccessBlocked() throws Exception {
            // User 2 trying to read User 1's pantry
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/pantry")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());

            // User 2 trying to modify User 1's pantry
            mockMvc.perform(post("/api/v1/users/" + user1Id + "/pantry")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ingredientId\":1,\"quantity\":2.0,\"unit\":\"PIECES\"}"))
                    .andExpect(status().isForbidden());

            // User 2 trying to read User 1's favorites
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/favorites")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());

            // User 2 trying to read User 1's preferences
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/preferences")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());

            // User 2 trying to read User 1's profile
            mockMvc.perform(get("/api/v1/users/" + user1Id)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authenticated owner access succeeds with HTTP 200")
        void testAuthenticatedOwnerAccessSuccess() throws Exception {
            // User 1 reading own pantry
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/pantry")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());

            // User 1 reading own favorites
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/favorites")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());

            // User 1 reading own preferences
            mockMvc.perform(get("/api/v1/users/" + user1Id + "/preferences")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());

            // User 1 reading own profile
            mockMvc.perform(get("/api/v1/users/" + user1Id)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2. Dietary Filter: Normalization, All mapping, and 400 Bad Request on invalid")
    class DietaryFilterTests {

        @Test
        @DisplayName("DietaryType enum parses supported values correctly")
        void testDietaryTypeParsing() {
            assertEquals(DietaryType.VEGETARIAN, DietaryType.fromValue("Vegetarian"));
            assertEquals(DietaryType.VEGETARIAN, DietaryType.fromValue("veg"));
            assertEquals(DietaryType.NON_VEGETARIAN, DietaryType.fromValue("Non-Vegetarian"));
            assertEquals(DietaryType.NON_VEGETARIAN, DietaryType.fromValue("non-veg"));
            assertEquals(DietaryType.NON_VEGETARIAN, DietaryType.fromValue("non_vegetarian"));
            assertEquals(DietaryType.VEGAN, DietaryType.fromValue("Vegan"));
            assertEquals(DietaryType.EGGETARIAN, DietaryType.fromValue("Eggetarian"));
            assertEquals(DietaryType.GLUTEN_FREE, DietaryType.fromValue("Gluten-Free"));

            // 'All' maps to null (no restriction)
            assertNull(DietaryType.fromValue("All"));
            assertNull(DietaryType.fromValue("all"));
            assertNull(DietaryType.fromValue(""));
            assertNull(DietaryType.fromValue(null));

            // Invalid value throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> DietaryType.fromValue("InvalidDiet"));
            assertThrows(IllegalArgumentException.class, () -> DietaryType.fromValue("Carnivore"));
        }

        @Test
        @DisplayName("GET /recipes with valid dietary filter returns 200 OK")
        void testGetRecipesWithValidDietaryFilter() throws Exception {
            mockMvc.perform(get("/api/v1/recipes?dietaryType=Vegetarian"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/recipes?dietaryType=Non-Vegetarian"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/recipes?dietaryType=All"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /recipes with invalid dietary filter returns HTTP 400 Bad Request")
        void testGetRecipesWithInvalidDietaryFilter() throws Exception {
            mockMvc.perform(get("/api/v1/recipes?dietaryType=InvalidDiet"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("POST /recommendations/by-ingredients with invalid dietaryType JSON returns HTTP 400 Bad Request")
        void testRecommendationWithInvalidDietaryJSON() throws Exception {
            mockMvc.perform(post("/api/v1/recommendations/by-ingredients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ingredients\":[\"Tomato\"],\"dietaryType\":\"InvalidDiet\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("3. Cuisine Matching: Regional matching and false-positive protection")
    class CuisineMatchingTests {

        @Test
        @DisplayName("North Indian matches North Indian Recipes and North Indian")
        void testNorthIndianMatching() {
            assertTrue(recommendationService.matchesCuisine("North Indian Recipes", List.of("North Indian")));
            assertTrue(recommendationService.matchesCuisine("North Indian", List.of("North Indian Recipes")));
            assertTrue(recommendationService.matchesCuisine("North Indian Recipes", List.of("north indian")));
        }

        @Test
        @DisplayName("Regional Indian cuisines match when Indian is requested")
        void testBroadIndianMatching() {
            assertTrue(recommendationService.matchesCuisine("North Indian Recipes", List.of("Indian")));
            assertTrue(recommendationService.matchesCuisine("South Indian Recipes", List.of("Indian")));
            assertTrue(recommendationService.matchesCuisine("Bengali Recipes", List.of("Indian")));
            assertTrue(recommendationService.matchesCuisine("Gujarati Recipes\ufeff", List.of("Indian")));
        }

        @Test
        @DisplayName("Unrelated cuisines are not matched (no weakening of matching)")
        void testUnrelatedCuisinesNotMatched() {
            assertFalse(recommendationService.matchesCuisine("South Indian Recipes", List.of("North Indian")));
            assertFalse(recommendationService.matchesCuisine("Italian Recipes", List.of("North Indian")));
            assertFalse(recommendationService.matchesCuisine("Continental", List.of("North Indian")));
            assertFalse(recommendationService.matchesCuisine("Mexican", List.of("North Indian")));
        }

        @Test
        @DisplayName("All option means no cuisine restriction")
        void testAllCuisineOption() {
            assertTrue(recommendationService.matchesCuisine("North Indian Recipes", List.of("All")));
            assertTrue(recommendationService.matchesCuisine("Italian Recipes", List.of("all")));
            assertTrue(recommendationService.matchesCuisine("Chinese", Collections.emptyList()));
        }

        @Test
        @DisplayName("Dynamic cuisines endpoint GET /api/v1/recipes/cuisines returns 200 OK with distinct cuisines")
        void testGetDistinctCuisinesEndpoint() throws Exception {
            mockMvc.perform(get("/api/v1/recipes/cuisines"))
                    .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
