package com.inchef.favorite;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

import com.inchef.common.ApiResponse;
import com.inchef.security.SecurityUtils;

@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * Retrieves the authenticated user's favorites list.
     * Supports both /api/v1/favorites and /api/v1/users/{userId}/favorites
     */
    @GetMapping({"/api/v1/users/{userId}/favorites", "/api/v1/favorites"})
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getUserFavorites(
            @PathVariable(required = false) Long userId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        if (userId != null) {
            SecurityUtils.validateUserAccess(userId);
        }
        List<FavoriteResponse> favorites = favoriteService.getUserFavorites(authUserId);
        return ResponseEntity.ok(ApiResponse.ok(favorites));
    }

    /**
     * Adds a recipe to the authenticated user's favorites.
     * Supports both /api/v1/favorites/{recipeId} and /api/v1/users/{userId}/favorites/{recipeId}
     */
    @PostMapping({"/api/v1/users/{userId}/favorites/{recipeId}", "/api/v1/favorites/{recipeId}"})
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @PathVariable(required = false) Long userId,
            @PathVariable Long recipeId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        if (userId != null) {
            SecurityUtils.validateUserAccess(userId);
        }
        FavoriteResponse favorite = favoriteService.addFavorite(authUserId, recipeId);
        return new ResponseEntity<>(ApiResponse.created("Recipe added to favorites", favorite), HttpStatus.CREATED);
    }

    /**
     * Removes a recipe from the authenticated user's favorites.
     * Supports both /api/v1/favorites/{recipeId} and /api/v1/users/{userId}/favorites/{recipeId}
     */
    @DeleteMapping({"/api/v1/users/{userId}/favorites/{recipeId}", "/api/v1/favorites/{recipeId}"})
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable(required = false) Long userId,
            @PathVariable Long recipeId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        if (userId != null) {
            SecurityUtils.validateUserAccess(userId);
        }
        favoriteService.removeFavorite(authUserId, recipeId);
        return ResponseEntity.ok(ApiResponse.ok("Recipe removed from favorites", null));
    }

    /**
     * Checks whether a recipe is favorited by the authenticated user.
     * Supports both /api/v1/favorites/{recipeId}/check and /api/v1/users/{userId}/favorites/{recipeId}/check
     */
    @GetMapping({"/api/v1/users/{userId}/favorites/{recipeId}/check", "/api/v1/favorites/{recipeId}/check"})
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFavorite(
            @PathVariable(required = false) Long userId,
            @PathVariable Long recipeId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        if (userId != null) {
            SecurityUtils.validateUserAccess(userId);
        }
        boolean isFav = favoriteService.isFavorite(authUserId, recipeId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isFavorite", isFav)));
    }

    /**
     * Retrieves the count of favorites for the authenticated user.
     * Supports both /api/v1/favorites/count and /api/v1/users/{userId}/favorites/count
     */
    @GetMapping({"/api/v1/users/{userId}/favorites/count", "/api/v1/favorites/count"})
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFavoriteCount(
            @PathVariable(required = false) Long userId) {
        Long authUserId = SecurityUtils.getRequiredCurrentUserId();
        if (userId != null) {
            SecurityUtils.validateUserAccess(userId);
        }
        long count = favoriteService.getUserFavoriteCount(authUserId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    /**
     * Retrieves the count of favorites for a specific recipe across all users.
     */
    @GetMapping("/api/v1/recipes/{recipeId}/favorites/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRecipeFavoriteCount(
            @PathVariable Long recipeId) {
        long count = favoriteService.getRecipeFavoriteCount(recipeId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }
}
