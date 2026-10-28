package com.inchef.soap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.inchef.exception.ResourceNotFoundException;
import com.inchef.favorite.FavoriteRepository;
import com.inchef.ingredient.IngredientRepository;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;
import com.inchef.recommendation.RecommendationRepository;
import com.inchef.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class RecipeReportService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecommendationRepository recommendationRepository;
    private final FavoriteRepository favoriteRepository;

    public RecipeReportService(UserRepository userRepository,
                               RecipeRepository recipeRepository,
                               IngredientRepository ingredientRepository,
                               RecommendationRepository recommendationRepository,
                               FavoriteRepository favoriteRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.recommendationRepository = recommendationRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public GetSystemReportResponse generateSystemReport(GetSystemReportRequest request) {
        long totalUsers = userRepository.count();
        long totalRecipes = recipeRepository.count();
        long totalIngredients = ingredientRepository.count();
        long totalRecommendations = recommendationRepository.count();

        List<Recipe> allRecipes = recipeRepository.findAll();

        // Compute cuisine counts
        Map<String, Long> cuisineMap = new HashMap<>();
        for (Recipe r : allRecipes) {
            String c = r.getCuisine() != null ? r.getCuisine() : "Other";
            cuisineMap.put(c, cuisineMap.getOrDefault(c, 0L) + 1);
        }

        List<CuisineStat> cuisineStats = cuisineMap.entrySet().stream()
                .map(e -> CuisineStat.builder().cuisine(e.getKey()).recipeCount(e.getValue()).build())
                .sorted(Comparator.comparingLong(CuisineStat::getRecipeCount).reversed())
                .collect(Collectors.toList());

        // Top favorited recipes
        List<RecipePopularityStat> topFavs = allRecipes.stream()
                .map(r -> RecipePopularityStat.builder()
                        .recipeId(r.getId())
                        .title(r.getTitle())
                        .cuisine(r.getCuisine())
                        .favoriteCount(favoriteRepository.countByRecipeId(r.getId()))
                        .build())
                .sorted(Comparator.comparingLong(RecipePopularityStat::getFavoriteCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return GetSystemReportResponse.builder()
                .totalUsers(totalUsers)
                .totalRecipes(totalRecipes)
                .totalIngredients(totalIngredients)
                .totalRecommendations(totalRecommendations)
                .cuisineStats(cuisineStats)
                .topFavoritedRecipes(topFavs)
                .generatedAt(generatedAt)
                .build();
    }

    public GetRecipePopularityReportResponse generateRecipePopularityReport(GetRecipePopularityReportRequest request) {
        Recipe recipe = recipeRepository.findByIdWithIngredients(request.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", request.getRecipeId()));

        long favCount = favoriteRepository.countByRecipeId(recipe.getId());
        int ingredientCount = recipe.getRecipeIngredients() != null ? recipe.getRecipeIngredients().size() : 0;

        return GetRecipePopularityReportResponse.builder()
                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())
                .cuisine(recipe.getCuisine())
                .favoriteCount(favCount)
                .ingredientCount(ingredientCount)
                .isPublished(recipe.getIsPublished() != null && recipe.getIsPublished())
                .build();
    }
}
