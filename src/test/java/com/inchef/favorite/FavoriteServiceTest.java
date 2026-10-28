package com.inchef.favorite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.preference.DietaryType;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeService;
import com.inchef.recipe.RecipeSummaryResponse;
import com.inchef.user.User;
import com.inchef.user.UserService;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserService userService;

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private FavoriteService favoriteService;

    private User sampleUser;
    private Recipe sampleRecipe;
    private Favorite sampleFavorite;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Chef User")
                .email("chef@inchef.com")
                .build();

        sampleRecipe = Recipe.builder()
                .id(100L)
                .title("Chicken Biryani")
                .translatedRecipeName("Authentic Chicken Biryani")
                .description("Aromatic basmati rice cooked with spices and chicken")
                .cuisine("Indian")
                .mealType(MealType.DINNER)
                .dietaryType(DietaryType.NON_VEGETARIAN)
                .difficulty(DifficultyLevel.MEDIUM)
                .prepTimeMinutes(20)
                .cookTimeMinutes(40)
                .totalTimeMinutes(60)
                .servings(4)
                .calories(550)
                .imageUrl("https://images.example.com/biryani.jpg")
                .isPublished(true)
                .build();

        sampleFavorite = Favorite.builder()
                .id(10L)
                .user(sampleUser)
                .recipe(sampleRecipe)
                .savedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully save a favorite and return full recipe details in DTO")
    void testAddFavorite_Success() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(recipeService.findEntityById(100L)).thenReturn(sampleRecipe);
        when(favoriteRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(sampleFavorite);
        when(recipeService.mapToSummaryResponse(sampleRecipe, null)).thenReturn(
                RecipeSummaryResponse.builder()
                        .id(100L)
                        .title("Chicken Biryani")
                        .cuisine("Indian")
                        .imageUrl("https://images.example.com/biryani.jpg")
                        .build()
        );

        FavoriteResponse response = favoriteService.addFavorite(1L, 100L);

        assertNotNull(response);
        assertEquals(10L, response.getFavoriteId());
        assertEquals(100L, response.getRecipeId());
        assertEquals("Chicken Biryani", response.getTitle());
        assertEquals("Authentic Chicken Biryani", response.getTranslatedRecipeName());
        assertEquals("Aromatic basmati rice cooked with spices and chicken", response.getDescription());
        assertEquals("Indian", response.getCuisine());
        assertEquals("DINNER", response.getCourse());
        assertEquals(DietaryType.NON_VEGETARIAN, response.getDietaryType());
        assertEquals(60, response.getTotalTimeMinutes());
        assertEquals("https://images.example.com/biryani.jpg", response.getImageUrl());
        assertTrue(response.getIsFavorite());
        assertNotNull(response.getRecipe());

        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should prevent duplicate saves by throwing ConflictException")
    void testAddFavorite_DuplicateThrowsConflictException() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(recipeService.findEntityById(100L)).thenReturn(sampleRecipe);
        when(favoriteRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.of(sampleFavorite));

        assertThrows(ConflictException.class, () -> favoriteService.addFavorite(1L, 100L));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should retrieve complete recipe details for user's favorites")
    void testGetUserFavorites_CompleteDetails() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(favoriteRepository.findByUserIdWithRecipe(1L)).thenReturn(List.of(sampleFavorite));
        when(recipeService.mapToSummaryResponse(sampleRecipe, null)).thenReturn(
                RecipeSummaryResponse.builder()
                        .id(100L)
                        .title("Chicken Biryani")
                        .cuisine("Indian")
                        .imageUrl("https://images.example.com/biryani.jpg")
                        .build()
        );

        List<FavoriteResponse> list = favoriteService.getUserFavorites(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
        FavoriteResponse fav = list.get(0);
        assertEquals(100L, fav.getRecipeId());
        assertEquals("Chicken Biryani", fav.getTitle());
        assertEquals("https://images.example.com/biryani.jpg", fav.getImageUrl());
        assertEquals("Indian", fav.getCuisine());
        assertEquals("DINNER", fav.getCourse());
        assertEquals(60, fav.getTotalTimeMinutes());
    }

    @Test
    @DisplayName("Should return correct favorite counts")
    void testGetFavoriteCounts() {
        when(favoriteRepository.countByUserId(1L)).thenReturn(7L);
        when(favoriteRepository.countByRecipeId(100L)).thenReturn(3L);

        assertEquals(7L, favoriteService.getUserFavoriteCount(1L));
        assertEquals(3L, favoriteService.getRecipeFavoriteCount(100L));
    }

    @Test
    @DisplayName("Should remove favorite by recipeId while preserving recipe")
    void testRemoveFavorite_ByRecipeId() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(favoriteRepository.existsByUserIdAndRecipeId(1L, 100L)).thenReturn(true);

        favoriteService.removeFavorite(1L, 100L);

        verify(favoriteRepository, times(1)).deleteByUserIdAndRecipeId(1L, 100L);
        verify(favoriteRepository, never()).delete(any());
        // Verify recipe is never deleted
        verify(recipeService, never()).deleteRecipe(anyLong());
    }

    @Test
    @DisplayName("Should remove favorite by favoriteId while preserving recipe")
    void testRemoveFavorite_ByFavoriteId() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(favoriteRepository.existsByUserIdAndRecipeId(1L, 10L)).thenReturn(false);
        when(favoriteRepository.findById(10L)).thenReturn(Optional.of(sampleFavorite));

        favoriteService.removeFavorite(1L, 10L);

        verify(favoriteRepository, times(1)).delete(sampleFavorite);
        verify(recipeService, never()).deleteRecipe(anyLong());
    }

    @Test
    @DisplayName("Should prevent cross-user deletion when removing by favoriteId")
    void testRemoveFavorite_CrossUserBlocked() {
        User otherUser = User.builder().id(999L).build();
        Favorite otherFavorite = Favorite.builder().id(10L).user(otherUser).recipe(sampleRecipe).build();

        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(favoriteRepository.existsByUserIdAndRecipeId(1L, 10L)).thenReturn(false);
        when(favoriteRepository.findById(10L)).thenReturn(Optional.of(otherFavorite));

        assertThrows(ResourceNotFoundException.class, () -> favoriteService.removeFavorite(1L, 10L));
        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when removing non-existent favorite")
    void testRemoveFavorite_NotFound() {
        when(userService.findEntityById(1L)).thenReturn(sampleUser);
        when(favoriteRepository.existsByUserIdAndRecipeId(1L, 999L)).thenReturn(false);
        when(favoriteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> favoriteService.removeFavorite(1L, 999L));
    }
}
