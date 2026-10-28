package com.inchef.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import com.inchef.exception.BadRequestException;
import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.loginhistory.LoginHistoryService;
import com.inchef.security.JwtTokenProvider;
import com.inchef.user.User;
import com.inchef.user.UserRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final GoogleAuthService googleAuthService;
    private final LoginHistoryService loginHistoryService;

    @Value("${inchef.auth.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${inchef.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       GoogleAuthService googleAuthService) {
        this(userRepository, passwordEncoder, tokenProvider, googleAuthService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       GoogleAuthService googleAuthService,
                       LoginHistoryService loginHistoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.googleAuthService = googleAuthService;
        this.loginHistoryService = loginHistoryService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .email(cleanEmail)
                .name(request.getName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider("LOCAL")
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());
        setAuthCookie(response, token);

        return toAuthResponse(savedUser, "Registration successful");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (userOpt.isEmpty()) {
            // Timing-attack mitigation: perform a dummy hash check so timing is indistinguishable
            passwordEncoder.matches("dummyPassword", "$2a$10$dummyHashToMitigateTimingAttacks1234567890123456789012");
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userOpt.get();

        // Check Lockout
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = Duration.between(LocalDateTime.now(), user.getLockoutUntil()).toMinutes() + 1;
            throw new LockedException("Account is temporarily locked. Please try again in " + remainingMinutes + " minutes.");
        }

        // Verify password
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                log.warn("User {} locked out for {} minutes after {} failed attempts", cleanEmail, lockoutMinutes, attempts);
            }
            userRepository.save(user);
            if (loginHistoryService != null) {
                loginHistoryService.recordLogin(user, "PASSWORD", "FAILED");
            }
            throw new BadCredentialsException("Invalid email or password");
        }

        // Reset lockout and failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        if (loginHistoryService != null) {
            loginHistoryService.recordLogin(user, "PASSWORD", "SUCCESS");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        setAuthCookie(response, token);

        return toAuthResponse(user, "Login successful");
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request, HttpServletResponse response) {
        GoogleIdToken.Payload payload = googleAuthService.verifyToken(request.getIdToken());

        String email = payload.getEmail().trim().toLowerCase();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            // Safe linking: do NOT overwrite existing passwordHash or drop existing user data
            if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                user.setGoogleId(googleId);
            }
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(picture);
            }
            user.setEmailVerified(true);
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            user = userRepository.save(user);
            log.info("Authenticated existing user {} via Google ID Token", email);
        } else {
            // New account created via verified Google identity
            user = User.builder()
                    .email(email)
                    .name(name != null && !name.isBlank() ? name.trim() : email.split("@")[0])
                    .avatarUrl(picture)
                    .authProvider("GOOGLE")
                    .googleId(googleId)
                    .emailVerified(true)
                    .failedLoginAttempts(0)
                    .build();
            user = userRepository.save(user);
            log.info("Created new user {} via Google OAuth Identity", email);
        }

        if (loginHistoryService != null) {
            loginHistoryService.recordLogin(user, "GOOGLE", "SUCCESS");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        setAuthCookie(response, token);

        return toAuthResponse(user, "Google login successful");
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(tokenProvider.getCookieName(), "")
                .httpOnly(true)
                .secure(false) // Set to true in HTTPS production
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public AuthResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toAuthResponse(user, "Authenticated");
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(tokenProvider.getCookieName(), token)
                .httpOnly(true)
                .secure(false) // Set to true in HTTPS production
                .path("/")
                .maxAge(tokenProvider.getExpirationMs() / 1000)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private AuthResponse toAuthResponse(User user, String message) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getAuthProvider(),
                user.getEmailVerified(),
                message
        );
    }
}
