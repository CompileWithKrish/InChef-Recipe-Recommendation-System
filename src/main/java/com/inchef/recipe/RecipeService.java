package com.inchef.recipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.inchef.exception.ResourceNotFoundException;
import com.inchef.favorite.FavoriteRepository;
import com.inchef.ingredient.Ingredient;
import com.inchef.ingredient.IngredientService;
import com.inchef.preference.DietaryType;

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
    public List<String> getDistinctCuisines() {
        List<String> rawCuisines = recipeRepository.findDistinctCuisines();
        Set<String> cleaned = new LinkedHashSet<>();
        for (String c : rawCuisines) {
            if (c != null && !c.isBlank()) {
                String clean = c.replaceAll("[\\uFEFF\\u200B-\\u200D]", "").trim();
                if (!clean.isEmpty()) {
                    cleaned.add(clean);
                }
            }
        }
        return new ArrayList<>(cleaned);
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
        String cleanQuery = query != null && !query.isBlank() && !query.trim().equalsIgnoreCase("all")
                ? query.replaceAll("[-_]+", " ").trim()
                : null;
        String cleanCuisine = cuisine != null && !cuisine.isBlank() && !cuisine.equalsIgnoreCase("all")
                ? cuisine.trim()
                : null;
        Page<Recipe> recipes = recipeRepository.searchRecipes(
                cleanQuery,
                cleanCuisine,
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
                .translatedRecipeName(recipe.getTranslatedRecipeName())
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
                .translatedRecipeName(recipe.getTranslatedRecipeName())
                .ingredientText(recipe.getIngredientText())
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
                .instructionSteps(CookingProgressService.parseInstructionSteps(recipe.getInstructions()))
                .imageUrl(recipe.getImageUrl())
                .videoUrl(recipe.getVideoUrl())
                .sourceUrl(recipe.getSourceUrl())
                .isFavorite(isFav)
                .favoriteCount(favCount)
                .ingredients(ingredients)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
