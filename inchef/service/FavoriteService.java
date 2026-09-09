package com.inchef.service;

import com.inchef.dto.response.FavoriteResponse;
import com.inchef.dto.response.RecipeSummaryResponse;
import com.inchef.entity.Favorite;
import com.inchef.entity.Recipe;
import com.inchef.entity.User;
import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            throw new ConflictException("Recipe is already in user favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .recipe(recipe)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        return mapToResponse(saved);
    }

    public void removeFavorite(Long userId, Long recipeId) {
        userService.findEntityById(userId);
        if (!favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            throw new ResourceNotFoundException("Favorite not found for recipe id: " + recipeId);
        }
        favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long recipeId) {
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    private FavoriteResponse mapToResponse(Favorite fav) {
        RecipeSummaryResponse recipeSummary = recipeService.mapToSummaryResponse(fav.getRecipe(), fav.getUser().getId());
        recipeSummary.setIsFavorite(true);

        return FavoriteResponse.builder()
                .id(fav.getId())
                .userId(fav.getUser().getId())
                .recipe(recipeSummary)
                .savedAt(fav.getSavedAt())
                .build();
    }
}
