package com.inchef.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.inchef.exception.BadRequestException;
import com.inchef.loginhistory.LoginHistoryRepository;
import com.inchef.loginhistory.LoginHistoryService;
import com.inchef.security.JwtTokenProvider;
import com.inchef.security.SecurityUtils;
import com.inchef.security.UserPrincipal;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class GoogleAuthTest {

    private static final String CONFIGURED_CLIENT_ID = "429840807756-ibb4pfkg65prgh661iunp32qtppjhv1p.apps.googleusercontent.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    private GoogleIdTokenVerifier mockVerifier;
    private GoogleAuthService googleAuthService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockVerifier = Mockito.mock(GoogleIdTokenVerifier.class);
        googleAuthService = new GoogleAuthService(CONFIGURED_CLIENT_ID, mockVerifier);
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider, googleAuthService, loginHistoryService);
    }

    private GoogleIdToken createMockToken(String subject, String email, boolean emailVerified, String audience, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(subject);
        payload.setEmail(email);
        payload.setEmailVerified(emailVerified);
        payload.setAudience(audience != null ? audience : CONFIGURED_CLIENT_ID);
        payload.set("name", name);
        payload.set("picture", "https://example.com/avatar.jpg");

        GoogleIdToken token = Mockito.mock(GoogleIdToken.class);
        when(token.getPayload()).thenReturn(payload);
        return token;
    }

    @Test
    @DisplayName("Successful Google login creates a new account, issues JWT cookie, and returns safe user data")
    void testGoogleLoginNewUser() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = createMockToken("google-sub-001", "priya.google@example.com", true, CONFIGURED_CLIENT_ID, "Priya Sharma");
        when(mockVerifier.verify("valid-google-id-token")).thenReturn(mockToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-id-token");

        AuthResponse authResponse = authService.googleLogin(request, response);

        assertNotNull(authResponse);
        assertEquals("priya.google@example.com", authResponse.getEmail());
        assertEquals("Priya Sharma", authResponse.getName());
        assertEquals("GOOGLE", authResponse.getAuthProvider());
        assertTrue(authResponse.getEmailVerified());

        // Verify database entity
        User saved = userRepository.findByEmailIgnoreCase("priya.google@example.com").orElseThrow();
        assertEquals("google-sub-001", saved.getGoogleId());
        assertNull(saved.getPasswordHash(), "Password hash must be null for new Google OAuth accounts");

        // Verify HttpOnly auth cookie
        String cookieHeader = response.getHeader("Set-Cookie");
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("inchef_auth_token="));
        assertTrue(cookieHeader.contains("HttpOnly"));
    }

    @Test
    @DisplayName("Google login with existing email links Google ID without overwriting password or losing data")
    void testGoogleLoginExistingUserSafeLink() throws GeneralSecurityException, IOException {
        // Create user with normal registration
        MockHttpServletResponse regResp = new MockHttpServletResponse();
        RegisterRequest regReq = new RegisterRequest("existing@inchef.com", "Existing Cook", "OriginalPassword123");
        authService.register(regReq, regResp);

        User beforeLogin = userRepository.findByEmailIgnoreCase("existing@inchef.com").orElseThrow();
        String originalPasswordHash = beforeLogin.getPasswordHash();
        assertNotNull(originalPasswordHash);
        assertNull(beforeLogin.getGoogleId());

        // Login via Google with matching email
        GoogleIdToken mockToken = createMockToken("google-sub-002", "existing@inchef.com", true, CONFIGURED_CLIENT_ID, "Existing Cook");
        when(mockVerifier.verify("google-token-existing")).thenReturn(mockToken);

        MockHttpServletResponse googleResp = new MockHttpServletResponse();
        AuthResponse authResponse = authService.googleLogin(new GoogleLoginRequest("google-token-existing"), googleResp);

        assertNotNull(authResponse);
        assertEquals("existing@inchef.com", authResponse.getEmail());

        // Verify password hash was preserved
        User afterLogin = userRepository.findByEmailIgnoreCase("existing@inchef.com").orElseThrow();
        assertEquals("google-sub-002", afterLogin.getGoogleId());
        assertEquals(originalPasswordHash, afterLogin.getPasswordHash(), "Original password hash must never be modified by Google login");

        // Verify the user can still log in using regular email/password
        MockHttpServletResponse normalLoginResp = new MockHttpServletResponse();
        AuthResponse normalAuth = authService.login(new LoginRequest("existing@inchef.com", "OriginalPassword123"), normalLoginResp);
        assertNotNull(normalAuth);
        assertEquals("existing@inchef.com", normalAuth.getEmail());
    }

    @Test
    @DisplayName("Invalid or expired Google token is rejected with 401 BadCredentialsException")
    void testGoogleLoginInvalidToken() throws GeneralSecurityException, IOException {
        // Verifier returning null simulates invalid signature, expired token, or corrupt string
        when(mockVerifier.verify("expired-or-invalid-token")).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleLoginRequest request = new GoogleLoginRequest("expired-or-invalid-token");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.googleLogin(request, response));
        assertTrue(ex.getMessage().contains("Invalid, expired, or unauthorized Google ID token"));
    }

    @Test
    @DisplayName("Google token with mismatched audience is rejected with 401 BadCredentialsException")
    void testGoogleLoginMismatchedAudience() throws GeneralSecurityException, IOException {
        // Token issued for a completely different client ID
        GoogleIdToken mockToken = createMockToken("google-sub-003", "hacker@example.com", true, "foreign-malicious-client-id.apps.googleusercontent.com", "Hacker");
        when(mockVerifier.verify("foreign-token")).thenReturn(mockToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleLoginRequest request = new GoogleLoginRequest("foreign-token");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.googleLogin(request, response));
        assertTrue(ex.getMessage().contains("audience mismatch"));
    }

    @Test
    @DisplayName("Google token with unverified email is rejected with 401 BadCredentialsException")
    void testGoogleLoginUnverifiedEmail() throws GeneralSecurityException, IOException {
        // Email verified flag is false
        GoogleIdToken mockToken = createMockToken("google-sub-004", "unverified@example.com", false, CONFIGURED_CLIENT_ID, "Unverified User");
        when(mockVerifier.verify("unverified-token")).thenReturn(mockToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleLoginRequest request = new GoogleLoginRequest("unverified-token");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.googleLogin(request, response));
        assertTrue(ex.getMessage().contains("email address is not verified"));
    }

    @Test
    @DisplayName("Blank or null token is rejected with BadRequestException")
    void testGoogleLoginBlankToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(BadRequestException.class, () ->
                authService.googleLogin(new GoogleLoginRequest(""), response));

        assertThrows(BadRequestException.class, () ->
                authService.googleLogin(new GoogleLoginRequest("   "), response));
    }

    @Test
    @DisplayName("Unconfigured Google Client ID throws IllegalStateException with clear remediation message")
    void testGoogleUnconfiguredClientId() {
        GoogleAuthService unconfiguredService = new GoogleAuthService("", null);
        AuthService localAuthService = new AuthService(userRepository, passwordEncoder, tokenProvider, unconfiguredService);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleLoginRequest request = new GoogleLoginRequest("any-token");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                localAuthService.googleLogin(request, response));
        assertTrue(ex.getMessage().contains("Google Client ID is not configured"));
    }

    @Test
    @DisplayName("Google-authenticated user has proper authorization to access own resources and denied for other users")
    void testGoogleUserAccessControl() throws GeneralSecurityException, IOException {
        GoogleIdToken mockToken = createMockToken("google-sub-auth", "authuser@example.com", true, CONFIGURED_CLIENT_ID, "Auth User");
        when(mockVerifier.verify("auth-token-123")).thenReturn(mockToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse authResponse = authService.googleLogin(new GoogleLoginRequest("auth-token-123"), response);

        Long myUserId = authResponse.getId();
        User myUser = userRepository.findById(myUserId).orElseThrow();

        // Simulate security context with Google user principal
        UserPrincipal principal = UserPrincipal.create(myUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        // Own data access permitted
        assertDoesNotThrow(() -> SecurityUtils.validateUserAccess(myUserId));

        // Other user data access blocked with 403 AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> SecurityUtils.validateUserAccess(myUserId + 999));
    }
}
