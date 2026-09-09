package com.inchef.controller;

import com.inchef.dto.request.RecipeCreateRequest;
import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.RecipeDetailResponse;
import com.inchef.dto.response.RecipeSummaryResponse;
import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> createRecipe(@Valid @RequestBody RecipeCreateRequest request) {
        RecipeDetailResponse response = recipeService.createRecipe(request);
        return new ResponseEntity<>(ApiResponse.created("Recipe created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecipeSummaryResponse>>> getRecipes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) DietaryType dietaryType,
            @RequestParam(required = false) MealType mealType,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String property = sortParts[0];

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        Page<RecipeSummaryResponse> recipes = recipeService.searchRecipes(
                query, cuisine, dietaryType, mealType, difficulty, maxTime, pageable, userId);

        return ResponseEntity.ok(ApiResponse.ok(recipes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> getRecipeById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        RecipeDetailResponse recipe = recipeService.getRecipeById(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(recipe));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeCreateRequest request) {
        RecipeDetailResponse updated = recipeService.updateRecipe(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Recipe updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok(ApiResponse.ok("Recipe deleted successfully", null));
    }
}
