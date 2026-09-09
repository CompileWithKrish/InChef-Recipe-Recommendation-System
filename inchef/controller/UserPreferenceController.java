package com.inchef.controller;

import com.inchef.dto.request.UserPreferenceRequest;
import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.UserPreferenceResponse;
import com.inchef.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getUserPreferences(@PathVariable Long userId) {
        UserPreferenceResponse preferences = userPreferenceService.getUserPreference(userId);
        return ResponseEntity.ok(ApiResponse.ok(preferences));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateUserPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        UserPreferenceResponse updated = userPreferenceService.saveOrUpdateUserPreference(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated successfully", updated));
    }
}
