package com.inchef.favorite;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeService;
import com.inchef.recipe.RecipeSummaryResponse;
import com.inchef.user.User;
import com.inchef.user.UserService;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final RecipeService recipeService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           UserService userService,
                           RecipeService recipeService) {
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
        this.recipeService = recipeService;
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getUserFavorites(Long userId) {
        userService.findEntityById(userId);
        return favoriteRepository.findByUserIdWithRecipe(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FavoriteResponse addFavorite(Long userId, Long recipeId) {
        User user = userService.findEntityById(userId);
        Recipe recipe = recipeService.findEntityById(recipeId);

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndRecipeId(userId, recipeId);
        if (existing.isPresent()) {
            throw new ConflictException("Recipe is already in user's favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .recipe(recipe)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        return mapToResponse(saved);
    }

    public void removeFavorite(Long userId, Long recipeOrFavoriteId) {
        userService.findEntityById(userId);

        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeOrFavoriteId)) {
            favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeOrFavoriteId);
            return;
        }

        // Support callers that pass the favoriteId instead of recipeId
        Optional<Favorite> byFavId = favoriteRepository.findById(recipeOrFavoriteId);
        if (byFavId.isPresent() && byFavId.get().getUser().getId().equals(userId)) {
            favoriteRepository.delete(byFavId.get());
            return;
        }

        throw new ResourceNotFoundException("Favorite not found for id: " + recipeOrFavoriteId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long recipeId) {
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    @Transactional(readOnly = true)
    public long getUserFavoriteCount(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getRecipeFavoriteCount(Long recipeId) {
        return favoriteRepository.countByRecipeId(recipeId);
    }

    public FavoriteResponse mapToResponse(Favorite fav) {
        Recipe r = fav.getRecipe();

        // Pass null currentUserId to avoid N+1 existsByUserIdAndRecipeId query
        RecipeSummaryResponse recipeSummary = recipeService.mapToSummaryResponse(r, null);
        recipeSummary.setIsFavorite(true);

        String course = r.getMealType() != null ? r.getMealType().name() : null;

        return FavoriteResponse.builder()
                .id(fav.getId())
                .favoriteId(fav.getId())
                .userId(fav.getUser().getId())
                .recipeId(r.getId())
                .title(r.getTitle())
                .translatedRecipeName(r.getTranslatedRecipeName())
                .description(r.getDescription())
                .cuisine(r.getCuisine())
                .course(course)
                .mealType(r.getMealType())
                .dietaryType(r.getDietaryType())
                .difficulty(r.getDifficulty())
                .prepTimeMinutes(r.getPrepTimeMinutes())
                .cookTimeMinutes(r.getCookTimeMinutes())
                .totalTimeMinutes(r.getTotalTimeMinutes())
                .servings(r.getServings())
                .calories(r.getCalories())
                .spiceLevel(r.getSpiceLevel())
                .imageUrl(r.getImageUrl())
                .isFavorite(true)
                .savedAt(fav.getSavedAt())
                .recipe(recipeSummary)
                .build();
    }
}
