package com.inchef.service;

import com.inchef.dto.request.RecipeCreateRequest;
import com.inchef.dto.request.RecipeIngredientRequest;
import com.inchef.dto.response.RecipeDetailResponse;
import com.inchef.dto.response.RecipeIngredientResponse;
import com.inchef.dto.response.RecipeSummaryResponse;
import com.inchef.entity.Ingredient;
import com.inchef.entity.Recipe;
import com.inchef.entity.RecipeIngredient;
import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.repository.FavoriteRepository;
import com.inchef.repository.RecipeIngredientRepository;
import com.inchef.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientService ingredientService;
    private final FavoriteRepository favoriteRepository;

    public RecipeService(RecipeRepository recipeRepository,
                         RecipeIngredientRepository recipeIngredientRepository,
                         IngredientService ingredientService,
                         FavoriteRepository favoriteRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientService = ingredientService;
        this.favoriteRepository = favoriteRepository;
    }

    public RecipeDetailResponse createRecipe(RecipeCreateRequest request) {
        int prepTime = request.getPrepTimeMinutes() != null ? request.getPrepTimeMinutes() : 0;
        int cookTime = request.getCookTimeMinutes() != null ? request.getCookTimeMinutes() : 0;
        int totalTime = request.getTotalTimeMinutes() != null ? request.getTotalTimeMinutes() : (prepTime + cookTime);

        Recipe recipe = Recipe.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .cuisine(request.getCuisine())
                .mealType(request.getMealType())
                .dietaryType(request.getDietaryType())
                .difficulty(request.getDifficulty())
                .prepTimeMinutes(prepTime)
                .cookTimeMinutes(cookTime)
                .totalTimeMinutes(totalTime)
                .servings(request.getServings() != null ? request.getServings() : 2)
                .calories(request.getCalories())
                .spiceLevel(request.getSpiceLevel())
                .instructions(request.getInstructions())
                .imageUrl(request.getImageUrl())
                .videoUrl(request.getVideoUrl())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();

        Recipe savedRecipe = recipeRepository.save(recipe);

        // Process ingredients
        if (request.getIngredients() != null) {
            for (RecipeIngredientRequest rir : request.getIngredients()) {
                Ingredient ingredient;
                if (rir.getIngredientId() != null) {
                    ingredient = ingredientService.findEntityById(rir.getIngredientId());
                } else if (rir.getIngredientName() != null && !rir.getIngredientName().isBlank()) {
                    ingredient = ingredientService.getOrCreateIngredient(rir.getIngredientName(), "General", rir.getUnit());
                } else {
                    continue;
                }

                RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                        .recipe(savedRecipe)
                        .ingredient(ingredient)
                        .quantity(rir.getQuantity())
                        .unit(rir.getUnit())
                        .isOptional(rir.getIsOptional() != null && rir.getIsOptional())
                        .notes(rir.getNotes())
                        .build();

                savedRecipe.addRecipeIngredient(recipeIngredient);
                recipeIngredientRepository.save(recipeIngredient);
            }
        }

        return mapToDetailResponse(savedRecipe, null);
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getRecipeById(Long id, Long currentUserId) {
        Recipe recipe = recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", id));
        return mapToDetailResponse(recipe, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> searchRecipes(String query,
                                                     String cuisine,
                                                     DietaryType dietaryType,
                                                     MealType mealType,
                                                     DifficultyLevel difficulty,
                                                     Integer maxTime,
                                                     Pageable pageable,
                                                     Long currentUserId) {
        Page<Recipe> recipes = recipeRepository.searchRecipes(
                query != null && !query.isBlank() ? query.trim() : null,
                cuisine != null && !cuisine.isBlank() ? cuisine.trim() : null,
                dietaryType,
                mealType,
                difficulty,
                maxTime,
                pageable
        );

        return recipes.map(r -> mapToSummaryResponse(r, currentUserId));
    }

    public RecipeDetailResponse updateRecipe(Long id, RecipeCreateRequest request) {
        Recipe recipe = recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", id));

        int prepTime = request.getPrepTimeMinutes() != null ? request.getPrepTimeMinutes() : 0;
        int cookTime = request.getCookTimeMinutes() != null ? request.getCookTimeMinutes() : 0;
        int totalTime = request.getTotalTimeMinutes() != null ? request.getTotalTimeMinutes() : (prepTime + cookTime);

        recipe.setTitle(request.getTitle().trim());
        recipe.setDescription(request.getDescription());
        recipe.setCuisine(request.getCuisine());
        recipe.setMealType(request.getMealType());
        recipe.setDietaryType(request.getDietaryType());
        recipe.setDifficulty(request.getDifficulty());
        recipe.setPrepTimeMinutes(prepTime);
        recipe.setCookTimeMinutes(cookTime);
        recipe.setTotalTimeMinutes(totalTime);
        recipe.setServings(request.getServings());
        recipe.setCalories(request.getCalories());
        recipe.setSpiceLevel(request.getSpiceLevel());
        recipe.setInstructions(request.getInstructions());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setVideoUrl(request.getVideoUrl());
        if (request.getIsPublished() != null) {
            recipe.setIsPublished(request.getIsPublished());
        }

        // Update ingredients if provided
        if (request.getIngredients() != null) {
            recipeIngredientRepository.deleteByRecipeId(id);
            recipe.getRecipeIngredients().clear();

            for (RecipeIngredientRequest rir : request.getIngredients()) {
                Ingredient ingredient;
                if (rir.getIngredientId() != null) {
                    ingredient = ingredientService.findEntityById(rir.getIngredientId());
                } else if (rir.getIngredientName() != null && !rir.getIngredientName().isBlank()) {
                    ingredient = ingredientService.getOrCreateIngredient(rir.getIngredientName(), "General", rir.getUnit());
                } else {
                    continue;
                }

                RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                        .recipe(recipe)
                        .ingredient(ingredient)
                        .quantity(rir.getQuantity())
                        .unit(rir.getUnit())
                        .isOptional(rir.getIsOptional() != null && rir.getIsOptional())
                        .notes(rir.getNotes())
                        .build();

                recipe.addRecipeIngredient(recipeIngredient);
                recipeIngredientRepository.save(recipeIngredient);
            }
        }

        Recipe updated = recipeRepository.save(recipe);
        return mapToDetailResponse(updated, null);
    }

    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", id));
        recipeRepository.delete(recipe);
    }

    @Transactional(readOnly = true)
    public Recipe findEntityById(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", id));
    }

    public RecipeSummaryResponse mapToSummaryResponse(Recipe recipe, Long currentUserId) {
        boolean isFav = false;
        if (currentUserId != null) {
            isFav = favoriteRepository.existsByUserIdAndRecipeId(currentUserId, recipe.getId());
        }

        return RecipeSummaryResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .cuisine(recipe.getCuisine())
                .mealType(recipe.getMealType())
                .dietaryType(recipe.getDietaryType())
                .difficulty(recipe.getDifficulty())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .totalTimeMinutes(recipe.getTotalTimeMinutes())
                .servings(recipe.getServings())
                .calories(recipe.getCalories())
                .spiceLevel(recipe.getSpiceLevel())
                .imageUrl(recipe.getImageUrl())
                .isFavorite(isFav)
                .createdAt(recipe.getCreatedAt())
                .build();
    }

    public RecipeDetailResponse mapToDetailResponse(Recipe recipe, Long currentUserId) {
        boolean isFav = false;
        if (currentUserId != null) {
            isFav = favoriteRepository.existsByUserIdAndRecipeId(currentUserId, recipe.getId());
        }
        long favCount = favoriteRepository.countByRecipeId(recipe.getId());

        List<RecipeIngredientResponse> ingredients = recipe.getRecipeIngredients() != null
                ? recipe.getRecipeIngredients().stream().map(ri -> RecipeIngredientResponse.builder()
                        .id(ri.getId())
                        .ingredientId(ri.getIngredient().getId())
                        .ingredientName(ri.getIngredient().getName())
                        .category(ri.getIngredient().getCategory())
                        .quantity(ri.getQuantity())
                        .unit(ri.getUnit())
                        .isOptional(ri.getIsOptional())
                        .notes(ri.getNotes())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return RecipeDetailResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .cuisine(recipe.getCuisine())
                .mealType(recipe.getMealType())
                .dietaryType(recipe.getDietaryType())
                .difficulty(recipe.getDifficulty())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .cookTimeMinutes(recipe.getCookTimeMinutes())
                .totalTimeMinutes(recipe.getTotalTimeMinutes())
                .servings(recipe.getServings())
                .calories(recipe.getCalories())
                .spiceLevel(recipe.getSpiceLevel())
                .instructions(recipe.getInstructions())
                .imageUrl(recipe.getImageUrl())
                .videoUrl(recipe.getVideoUrl())
                .isFavorite(isFav)
                .favoriteCount(favCount)
                .ingredients(ingredients)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
