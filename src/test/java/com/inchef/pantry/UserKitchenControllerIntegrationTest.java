package com.inchef.pantry;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.inchef.auth.AuthResponse;
import com.inchef.auth.AuthService;
import com.inchef.auth.RegisterRequest;
import com.inchef.common.MeasurementUnit;
import com.inchef.ingredient.Ingredient;
import com.inchef.ingredient.IngredientRepository;
import com.inchef.security.JwtTokenProvider;
import com.inchef.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class UserKitchenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UserIngredientRepository userIngredientRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long userId;
    private String userToken;
    private Ingredient sampleIngredient;

    @BeforeEach
    void setUp() {
        userIngredientRepository.deleteAll();

        String email = "kitchen_user_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthResponse auth = authService.register(new RegisterRequest(email, "Kitchen Chef", "Password123"), res);
        userId = auth.getId();
        userToken = jwtTokenProvider.generateToken(userId, email);

        sampleIngredient = ingredientRepository.save(Ingredient.builder()
                .name("Kashmiri Chilli")
                .category("Spices")
                .defaultUnit(MeasurementUnit.GRAMS)
                .build());
    }

    @Test
    @DisplayName("Should successfully add an ingredient with unit PIECES")
    void testAddIngredient_WithPieces() throws Exception {
        String json = """
            {
                "ingredientName": "Egg",
                "category": "Dairy & Eggs",
                "quantity": 6.0,
                "unit": "PIECES"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ingredientName", is("Egg")))
                .andExpect(jsonPath("$.data.quantity", is(6.0)))
                .andExpect(jsonPath("$.data.unit", is("PIECES")));
    }

    @Test
    @DisplayName("Should accept alias 'item' and map it safely to PIECES")
    void testAddIngredient_WithItemAlias() throws Exception {
        String json = """
            {
                "ingredientName": "Lemon",
                "category": "Fruits",
                "quantity": 2.0,
                "unit": "item"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ingredientName", is("Lemon")))
                .andExpect(jsonPath("$.data.quantity", is(2.0)))
                .andExpect(jsonPath("$.data.unit", is("PIECES")));
    }

    @Test
    @DisplayName("Should successfully add ingredients measured in GRAMS and CUPS")
    void testAddIngredient_WithGramsAndCups() throws Exception {
        // Add Basmati Rice in GRAMS
        String riceJson = """
            {
                "ingredientName": "Basmati Rice",
                "category": "Grains",
                "quantity": 500.0,
                "unit": "GRAMS"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(riceJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ingredientName", is("Basmati Rice")))
                .andExpect(jsonPath("$.data.quantity", is(500.0)))
                .andExpect(jsonPath("$.data.unit", is("GRAMS")));

        // Add Milk in CUPS
        String milkJson = """
            {
                "ingredientName": "Milk",
                "category": "Dairy & Eggs",
                "quantity": 2.5,
                "unit": "CUPS"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(milkJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ingredientName", is("Milk")))
                .andExpect(jsonPath("$.data.quantity", is(2.5)))
                .andExpect(jsonPath("$.data.unit", is("CUPS")));

        // Verify retrieval of pantry returns both items with correct values
        mockMvc.perform(get("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].ingredientName", containsInAnyOrder("Basmati Rice", "Milk")))
                .andExpect(jsonPath("$.data[*].unit", containsInAnyOrder("GRAMS", "CUPS")));
    }

    @Test
    @DisplayName("Should default unit to PIECES when unit is omitted")
    void testAddIngredient_DefaultUnit() throws Exception {
        String json = """
            {
                "ingredientName": "Onion",
                "category": "Vegetables",
                "quantity": 3.0
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ingredientName", is("Onion")))
                .andExpect(jsonPath("$.data.quantity", is(3.0)))
                .andExpect(jsonPath("$.data.unit", is("PIECES")));
    }

    @Test
    @DisplayName("Should return clean 400 Bad Request with clear validation message for invalid unit")
    void testAddIngredient_InvalidUnitReturnsClean400() throws Exception {
        String json = """
            {
                "ingredientName": "Ginger",
                "quantity": 1.0,
                "unit": "invalid_unit_xyz"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid measurement unit 'invalid_unit_xyz'")))
                .andExpect(jsonPath("$.message", not(containsString("Cannot deserialize value of type"))));
    }

    @Test
    @DisplayName("Should reject negative quantity with 400 Bad Request")
    void testAddIngredient_NegativeQuantityRejected() throws Exception {
        String json = """
            {
                "ingredientName": "Ginger",
                "quantity": -5.0,
                "unit": "GRAMS"
            }
            """;

        mockMvc.perform(post("/api/v1/users/" + userId + "/pantry")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
