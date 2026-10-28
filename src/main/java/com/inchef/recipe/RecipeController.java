package com.inchef.recipe;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;
import com.inchef.exception.BadRequestException;
import com.inchef.preference.DietaryType;
import com.inchef.security.SecurityUtils;

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

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RecipeSummaryResponse>>> searchRecipesDirectly(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String dietaryType,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        return getRecipes(query, cuisine, dietaryType, mealType, difficulty, maxTime, userId, page, size, sort);
    }

    @GetMapping("/cuisines")
    public ResponseEntity<ApiResponse<List<String>>> getCuisines() {
        List<String> cuisines = recipeService.getDistinctCuisines();
        return ResponseEntity.ok(ApiResponse.ok("Cuisines retrieved successfully", cuisines));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecipeSummaryResponse>>> getRecipes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String dietaryType,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        String parsedQuery = sanitizeFilter(query);
        String parsedCuisine = sanitizeFilter(cuisine);
        DietaryType parsedDietaryType = parseDietaryType(dietaryType);
        MealType parsedMealType = parseMealType(mealType);

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String property = sortParts[0];

        Long authUserId = SecurityUtils.getCurrentUserId();
        Long effectiveUserId = authUserId != null ? authUserId : userId;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        Page<RecipeSummaryResponse> recipes = recipeService.searchRecipes(
                parsedQuery, parsedCuisine, parsedDietaryType, parsedMealType, difficulty, maxTime, pageable, effectiveUserId);

        return ResponseEntity.ok(ApiResponse.ok(recipes));
    }

    private String sanitizeFilter(String val) {
        if (val == null || val.isBlank() || val.trim().equalsIgnoreCase("all")) {
            return null;
        }
        return val.trim();
    }

    private DietaryType parseDietaryType(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return DietaryType.fromValue(val);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private MealType parseMealType(String val) {
        String clean = sanitizeFilter(val);
        if (clean == null) return null;
        try {
            return MealType.valueOf(clean.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<RecipeDetailResponse>> getRecipeById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        Long authUserId = SecurityUtils.getCurrentUserId();
        Long effectiveUserId = authUserId != null ? authUserId : userId;
        RecipeDetailResponse recipe = recipeService.getRecipeById(id, effectiveUserId);
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
