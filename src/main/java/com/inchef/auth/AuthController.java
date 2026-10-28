package com.inchef.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.inchef.common.ApiResponse;
import com.inchef.security.UserPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful", auth));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in"));
        }
        AuthResponse auth = authService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved", auth));
    }

    @GetMapping("/google/config")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getGoogleConfig(
            @org.springframework.beans.factory.annotation.Value("${inchef.google.client-id:}") String clientId) {
        boolean configured = clientId != null && !clientId.trim().isEmpty();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("clientId", clientId != null ? clientId.trim() : "");
        data.put("configured", configured);
        return ResponseEntity.ok(ApiResponse.ok("Google OAuth configuration retrieved", data));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.googleLogin(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Google login successful", auth));
    }
}
