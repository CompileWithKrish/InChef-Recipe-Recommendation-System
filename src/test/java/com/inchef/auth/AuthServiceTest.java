package com.inchef.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

import com.inchef.exception.ConflictException;
import com.inchef.security.JwtTokenProvider;
import com.inchef.security.SecurityUtils;
import com.inchef.security.UserPrincipal;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Successful registration creates user with BCrypt hash and issues JWT cookie")
    void testSuccessfulRegistration() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RegisterRequest req = new RegisterRequest("newchef@inchef.com", "Chef Gordon", "Secret1234");

        AuthResponse auth = authService.register(req, response);

        assertNotNull(auth);
        assertNotNull(auth.getId());
        assertEquals("newchef@inchef.com", auth.getEmail());
        assertEquals("Chef Gordon", auth.getName());

        // Verify password hash in database
        User saved = userRepository.findById(auth.getId()).orElseThrow();
        assertNotNull(saved.getPasswordHash());
        assertNotEquals("Secret1234", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2a$") || saved.getPasswordHash().startsWith("$2b$"));

        // Verify cookie
        String cookieHeader = response.getHeader("Set-Cookie");
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("inchef_auth_token="));
        assertTrue(cookieHeader.contains("HttpOnly"));
    }

    @Test
    @DisplayName("Duplicate email registration throws ConflictException (case-insensitive)")
    void testDuplicateEmailRegistration() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RegisterRequest req1 = new RegisterRequest("duplicate@inchef.com", "Chef One", "Secret1234");
        authService.register(req1, response);

        RegisterRequest req2 = new RegisterRequest("DUPLICATE@INCHEF.COM", "Chef Two", "Secret5678");
        assertThrows(ConflictException.class, () -> authService.register(req2, response));
    }

    @Test
    @DisplayName("Successful login returns user and sets HttpOnly cookie")
    void testSuccessfulLogin() {
        MockHttpServletResponse regResp = new MockHttpServletResponse();
        authService.register(new RegisterRequest("loginuser@inchef.com", "Login User", "Password99"), regResp);

        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginRequest loginReq = new LoginRequest("loginuser@inchef.com", "Password99");
        AuthResponse auth = authService.login(loginReq, loginResp);

        assertNotNull(auth);
        assertEquals("loginuser@inchef.com", auth.getEmail());

        String cookie = loginResp.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("inchef_auth_token="));
    }

    @Test
    @DisplayName("Login with wrong password throws BadCredentialsException without revealing details")
    void testWrongPasswordLogin() {
        MockHttpServletResponse regResp = new MockHttpServletResponse();
        authService.register(new RegisterRequest("wrongpass@inchef.com", "User", "CorrectPass1"), regResp);

        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginRequest loginReq = new LoginRequest("wrongpass@inchef.com", "IncorrectPass2");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.login(loginReq, loginResp));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Login with non-existent email throws identical BadCredentialsException")
    void testNonExistentEmailLogin() {
        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginRequest loginReq = new LoginRequest("nonexistent@inchef.com", "AnyPassword123");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.login(loginReq, loginResp));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Account lock out triggers after 5 failed login attempts")
    void testAccountLockoutAfterMaxFailures() {
        MockHttpServletResponse regResp = new MockHttpServletResponse();
        authService.register(new RegisterRequest("lockout@inchef.com", "Lockout User", "PasswordValid1"), regResp);

        MockHttpServletResponse loginResp = new MockHttpServletResponse();
        LoginRequest badReq = new LoginRequest("lockout@inchef.com", "WrongPassword99");

        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(badReq, loginResp));
        }

        // 6th attempt should be locked
        assertThrows(LockedException.class, () -> authService.login(badReq, loginResp));
    }

    @Test
    @DisplayName("User A cannot access or modify User B data (403 AccessDeniedException)")
    void testCrossUserDataIsolation() {
        User userA = userRepository.save(User.builder().name("User A").email("usera@inchef.com").build());
        User userB = userRepository.save(User.builder().name("User B").email("userb@inchef.com").build());

        // Authenticate as User A
        UserPrincipal principalA = UserPrincipal.create(userA);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principalA, null, principalA.getAuthorities())
        );

        // Accessing User A's data should pass
        assertDoesNotThrow(() -> SecurityUtils.validateUserAccess(userA.getId()));

        // Accessing User B's data should throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> SecurityUtils.validateUserAccess(userB.getId()));
    }

    @Test
    @DisplayName("JWT validation recognizes valid and rejects invalid/tampered tokens")
    void testJwtValidation() {
        String token = tokenProvider.generateToken(999L, "tokenuser@inchef.com");
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(999L, tokenProvider.getUserIdFromToken(token));
        assertEquals("tokenuser@inchef.com", tokenProvider.getEmailFromToken(token));

        // Tampered token
        String tampered = token + "xyz";
        assertFalse(tokenProvider.validateToken(tampered));
    }
}
