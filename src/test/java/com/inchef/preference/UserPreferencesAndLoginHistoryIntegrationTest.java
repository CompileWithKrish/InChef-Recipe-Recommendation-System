package com.inchef.preference;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.inchef.auth.AuthResponse;
import com.inchef.auth.AuthService;
import com.inchef.auth.GoogleAuthService;
import com.inchef.auth.GoogleLoginRequest;
import com.inchef.auth.LoginRequest;
import com.inchef.auth.RegisterRequest;
import com.inchef.loginhistory.LoginHistory;
import com.inchef.loginhistory.LoginHistoryRepository;
import com.inchef.loginhistory.LoginHistoryService;
import com.inchef.security.JwtTokenProvider;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class UserPreferencesAndLoginHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Long user1Id;
    private String user1Token;
    private String user1Email;

    private Long user2Id;
    private String user2Token;
    private String user2Email;

    @BeforeEach
    void setUp() {
        loginHistoryRepository.deleteAll();

        // User 1
        user1Email = "pref_user1_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        AuthResponse auth1 = authService.register(new RegisterRequest(user1Email, "Preference User 1", "SecurePass123"), res1);
        user1Id = auth1.getId();
        user1Token = jwtTokenProvider.generateToken(user1Id, user1Email);

        // User 2
        user2Email = "pref_user2_" + System.nanoTime() + "@inchef.com";
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        AuthResponse auth2 = authService.register(new RegisterRequest(user2Email, "Preference User 2", "SecurePass123"), res2);
        user2Id = auth2.getId();
        user2Token = jwtTokenProvider.generateToken(user2Id, user2Email);
    }

    // ==========================================
    // PART A: PREFERENCES TESTS
    // ==========================================

    @Test
    @DisplayName("1. Saving one preferred cuisine persists as array and returns correct structure")
    void testSaveSinglePreferredCuisine() throws Exception {
        String payload = """
                {
                    "dietaryType": "VEGETARIAN",
                    "preferredSpiceLevel": "MEDIUM",
                    "preferredCuisines": ["North Indian"],
                    "allergies": [],
                    "maxCookingTime": 35
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(1)))
                .andExpect(jsonPath("$.data.preferredCuisines[0]").value("North Indian"))
                .andExpect(jsonPath("$.data.dietaryType").value("VEGETARIAN"))
                .andExpect(jsonPath("$.data.maxCookingTime").value(35));

        // Verify retrieval on simulated reload
        mockMvc.perform(get("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(1)))
                .andExpect(jsonPath("$.data.preferredCuisines[0]").value("North Indian"));
    }

    @Test
    @DisplayName("2. Saving multiple preferred cuisines persists all selected items")
    void testSaveMultiplePreferredCuisines() throws Exception {
        String payload = """
                {
                    "dietaryType": "VEGAN",
                    "preferredSpiceLevel": "SPICY",
                    "preferredCuisines": ["North Indian", "South Indian", "Punjabi", "Gujarati"],
                    "allergies": ["Peanuts"],
                    "maxCookingTime": 50
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(4)))
                .andExpect(jsonPath("$.data.preferredCuisines", containsInAnyOrder("North Indian", "South Indian", "Punjabi", "Gujarati")))
                .andExpect(jsonPath("$.data.allergies", contains("Peanuts")));
    }

    @Test
    @DisplayName("3. Clearing cuisine preferences with empty array or null clears saved cuisines")
    void testClearPreferences() throws Exception {
        // First set cuisines
        String initialPayload = """
                {
                    "dietaryType": "VEGETARIAN",
                    "preferredSpiceLevel": "MILD",
                    "preferredCuisines": ["Mughlai", "Bengali"],
                    "maxCookingTime": 40
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(2)));

        // Now clear with empty array
        String clearPayload = """
                {
                    "dietaryType": "VEGETARIAN",
                    "preferredSpiceLevel": "MILD",
                    "preferredCuisines": [],
                    "allergies": [],
                    "maxCookingTime": 40
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clearPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(0)));

        // Reload verification
        mockMvc.perform(get("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredCuisines", hasSize(0)));
    }

    @Test
    @DisplayName("4. Malformed payload with string instead of array or invalid values returns HTTP 400 Bad Request")
    void testMalformedPayloadReturns400() throws Exception {
        // String instead of Array for preferredCuisines
        String stringInsteadOfArray = """
                {
                    "dietaryType": "VEGETARIAN",
                    "preferredCuisines": "North Indian",
                    "maxCookingTime": 45
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stringInsteadOfArray))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("preferredCuisines")));

        // Negative cooking time validation error
        String negativeCookTime = """
                {
                    "dietaryType": "VEGETARIAN",
                    "preferredCuisines": ["North Indian"],
                    "maxCookingTime": -15
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + user1Id + "/preferences")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeCookTime))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // PART B: LOGIN HISTORY TESTS
    // ==========================================

    @Test
    @DisplayName("5. Successful password login is recorded in login_history exactly once")
    void testSuccessfulPasswordLoginRecordedOnce() {
        loginHistoryRepository.deleteAll();

        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthResponse loginRes = authService.login(new LoginRequest(user1Email, "SecurePass123"), res);
        assertNotNull(loginRes);

        List<LoginHistory> historyList = loginHistoryRepository.findAll();
        assertEquals(1, historyList.size(), "Login event must be recorded exactly once");

        LoginHistory event = historyList.get(0);
        assertEquals(user1Id, event.getUser().getId());
        assertEquals("PASSWORD", event.getAuthenticationMethod());
        assertEquals("SUCCESS", event.getStatus());
        assertNotNull(event.getLoginAt());
    }

    @Test
    @DisplayName("6. Successful Google Sign-In is recorded in login_history exactly once")
    void testSuccessfulGoogleLoginRecordedOnce() {
        loginHistoryRepository.deleteAll();

        // Setup verified google token
        GoogleIdTokenVerifier mockVerifier = Mockito.mock(GoogleIdTokenVerifier.class);
        GoogleAuthService googleAuthService = new GoogleAuthService("test-client-id", mockVerifier);

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-uid-12345");
        payload.setEmail(user1Email);
        payload.setEmailVerified(true);
        payload.setAudience("test-client-id");
        payload.set("name", "Google User");

        GoogleIdToken token = Mockito.mock(GoogleIdToken.class);
        when(token.getPayload()).thenReturn(payload);

        try {
            when(mockVerifier.verify("valid-mock-token")).thenReturn(token);
        } catch (Exception ignored) {}

        AuthService localAuthService = new AuthService(
                userRepository,
                Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                jwtTokenProvider,
                googleAuthService,
                loginHistoryService
        );

        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthResponse googleAuthRes = localAuthService.googleLogin(new GoogleLoginRequest("valid-mock-token"), res);
        assertNotNull(googleAuthRes);

        List<LoginHistory> historyList = loginHistoryRepository.findAll();
        assertEquals(1, historyList.size(), "Google login event must be recorded exactly once");
        assertEquals(user1Id, historyList.get(0).getUser().getId());
        assertEquals("GOOGLE", historyList.get(0).getAuthenticationMethod());
        assertEquals("SUCCESS", historyList.get(0).getStatus());
        assertEquals(1, historyList.size());
        assertEquals("GOOGLE", historyList.get(0).getAuthenticationMethod());
        assertEquals("SUCCESS", historyList.get(0).getStatus());
    }

    @Test
    @DisplayName("7. Login history sorting (newest first) and pagination")
    void testLoginHistorySortingAndPagination() throws Exception {
        loginHistoryRepository.deleteAll();

        // Create 5 login events with distinct timestamps
        User user = userRepository.findById(user1Id).orElseThrow();
        for (int i = 1; i <= 5; i++) {
            LoginHistory event = LoginHistory.builder()
                    .user(user)
                    .loginAt(java.time.Instant.now().minusSeconds((5 - i) * 60)) // event 5 is newest
                    .authenticationMethod(i % 2 == 0 ? "GOOGLE" : "PASSWORD")
                    .status("SUCCESS")
                    .build();
            loginHistoryRepository.save(event);
        }

        // Request page 0, size 2
        mockMvc.perform(get("/api/v1/users/me/login-history?page=0&size=2")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content[0].authenticationMethod").value("PASSWORD")) // Event 5 is newest
                .andExpect(jsonPath("$.data.content[0].timeZone").value("UTC"));

        // Request page 1, size 2
        mockMvc.perform(get("/api/v1/users/me/login-history?page=1&size=2")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    @Test
    @DisplayName("8. Unauthenticated login history access returns 401 Unauthorized")
    void testUnauthenticatedLoginHistoryReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/login-history"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/" + user1Id + "/login-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. Cross-user login history access returns 403 Forbidden")
    void testCrossUserLoginHistoryReturns403() throws Exception {
        // User 2 trying to read User 1's login history
        mockMvc.perform(get("/api/v1/users/" + user1Id + "/login-history")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
