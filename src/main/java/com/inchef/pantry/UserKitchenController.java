package com.inchef.pantry;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;
import com.inchef.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/users/{userId}/pantry")
public class UserKitchenController {

    private final UserIngredientService userIngredientService;

    public UserKitchenController(UserIngredientService userIngredientService) {
        this.userIngredientService = userIngredientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserIngredientResponse>>> getUserPantry(@PathVariable Long userId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        List<UserIngredientResponse> pantry = userIngredientService.getUserIngredients(authUserId);
        return ResponseEntity.ok(ApiResponse.ok(pantry));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserIngredientResponse>> addPantryItem(
            @PathVariable Long userId,
            @Valid @RequestBody UserIngredientRequest request) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        UserIngredientResponse item = userIngredientService.addOrUpdateUserIngredient(authUserId, request);
        return new ResponseEntity<>(ApiResponse.created("Pantry item added/updated successfully", item), HttpStatus.CREATED);
    }

    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<Void>> removePantryItem(
            @PathVariable Long userId,
            @PathVariable Long ingredientId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        userIngredientService.removeUserIngredient(authUserId, ingredientId);
        return ResponseEntity.ok(ApiResponse.ok("Pantry item removed successfully", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearPantry(@PathVariable Long userId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        SecurityUtils.validateUserAccess(userId);
        userIngredientService.clearUserIngredients(authUserId);
        return ResponseEntity.ok(ApiResponse.ok("Pantry cleared successfully", null));
    }
}
