package com.inchef.controller;

import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.FavoriteResponse;
import com.inchef.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/{userId}/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getUserFavorites(@PathVariable Long userId) {
        List<FavoriteResponse> favorites = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(ApiResponse.ok(favorites));
    }

    @PostMapping("/{recipeId}")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @PathVariable Long userId,
            @PathVariable Long recipeId) {
        FavoriteResponse favorite = favoriteService.addFavorite(userId, recipeId);
        return new ResponseEntity<>(ApiResponse.created("Recipe added to favorites", favorite), HttpStatus.CREATED);
    }

    @DeleteMapping("/{recipeId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long userId,
            @PathVariable Long recipeId) {
        favoriteService.removeFavorite(userId, recipeId);
        return ResponseEntity.ok(ApiResponse.ok("Recipe removed from favorites", null));
    }

    @GetMapping("/{recipeId}/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFavorite(
            @PathVariable Long userId,
            @PathVariable Long recipeId) {
        boolean isFav = favoriteService.isFavorite(userId, recipeId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isFavorite", isFav)));
    }
}
