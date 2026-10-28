package com.inchef.preference;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inchef.common.ApiResponse;
import com.inchef.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getUserPreferences(@PathVariable Long userId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        UserPreferenceResponse preferences = userPreferenceService.getUserPreference(authUserId);
        return ResponseEntity.ok(ApiResponse.ok(preferences));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateUserPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        UserPreferenceResponse updated = userPreferenceService.saveOrUpdateUserPreference(authUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated successfully", updated));
    }
}
