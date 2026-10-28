package com.inchef.recipe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.inchef.common.MeasurementUnit;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.favorite.FavoriteRepository;
import com.inchef.ingredient.Ingredient;
import com.inchef.ingredient.IngredientService;
import com.inchef.preference.DietaryType;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;

    @Mock
    private IngredientService ingredientService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe sampleRecipe;
    private Ingredient sampleIngredient;

    @BeforeEach
    void setUp() {
        sampleIngredient = Ingredient.builder()
                .id(10L)
                .name("Tomato")
                .category("Vegetables")
                .defaultUnit(MeasurementUnit.PIECES)
                .build();

        sampleRecipe = Recipe.builder()
                .id(1L)
                .title("Tomato Soup")
                .description("Warm comforting soup")
                .cuisine("American")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(20)
                .totalTimeMinutes(30)
                .isPublished(true)
                .build();
    }

    @Test
    @DisplayName("Should create recipe successfully and associate recipe ingredients")
    void testCreateRecipe() {
        RecipeCreateRequest request = RecipeCreateRequest.builder()
                .title("Tomato Soup")
                .description("Warm comforting soup")
                .cuisine("American")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.VEGETARIAN)
                .difficulty(DifficultyLevel.EASY)
                .prepTimeMinutes(10)
                .cookTimeMinutes(20)
                .instructions("Simmer tomatoes and blend")
                .ingredients(List.of(
                        RecipeIngredientRequest.builder()
                                .ingredientId(10L)
                                .quantity(4.0)
                                .unit(MeasurementUnit.PIECES)
                                .build()
                ))
                .build();

        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(ingredientService.findEntityById(10L)).thenReturn(sampleIngredient);

        RecipeDetailResponse response = recipeService.createRecipe(request);

        assertNotNull(response);
        assertEquals("Tomato Soup", response.getTitle());
        assertEquals(30, response.getTotalTimeMinutes());
        verify(recipeRepository, times(1)).save(any(Recipe.class));
        verify(recipeIngredientRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when recipe ID does not exist")
    void testGetRecipeNotFound() {
        when(recipeRepository.findByIdWithIngredients(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.getRecipeById(999L, null));
    }
}
