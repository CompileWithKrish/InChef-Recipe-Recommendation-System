package com.inchef.controller;

import com.inchef.dto.request.UserIngredientRequest;
import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.UserIngredientResponse;
import com.inchef.service.UserIngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/pantry")
public class UserKitchenController {

    private final UserIngredientService userIngredientService;

    public UserKitchenController(UserIngredientService userIngredientService) {
        this.userIngredientService = userIngredientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserIngredientResponse>>> getUserPantry(@PathVariable Long userId) {
        List<UserIngredientResponse> pantry = userIngredientService.getUserIngredients(userId);
        return ResponseEntity.ok(ApiResponse.ok(pantry));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserIngredientResponse>> addPantryItem(
            @PathVariable Long userId,
            @Valid @RequestBody UserIngredientRequest request) {
        UserIngredientResponse item = userIngredientService.addOrUpdateUserIngredient(userId, request);
        return new ResponseEntity<>(ApiResponse.created("Pantry item added/updated successfully", item), HttpStatus.CREATED);
    }

    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<Void>> removePantryItem(
            @PathVariable Long userId,
            @PathVariable Long ingredientId) {
        userIngredientService.removeUserIngredient(userId, ingredientId);
        return ResponseEntity.ok(ApiResponse.ok("Pantry item removed successfully", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearPantry(@PathVariable Long userId) {
        userIngredientService.clearUserIngredients(userId);
        return ResponseEntity.ok(ApiResponse.ok("Pantry cleared successfully", null));
    }
}
