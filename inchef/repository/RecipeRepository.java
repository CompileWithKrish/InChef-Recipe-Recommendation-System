package com.inchef.repository;

import com.inchef.entity.Recipe;
import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.recipeIngredients ri LEFT JOIN FETCH ri.ingredient WHERE r.id = :id")
    Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.recipeIngredients ri LEFT JOIN FETCH ri.ingredient WHERE r.isPublished = true")
    List<Recipe> findAllPublishedWithIngredients();

    Page<Recipe> findByIsPublishedTrue(Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true AND " +
           "(:query IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:cuisine IS NULL OR LOWER(r.cuisine) = LOWER(:cuisine)) AND " +
           "(:dietaryType IS NULL OR r.dietaryType = :dietaryType) AND " +
           "(:mealType IS NULL OR r.mealType = :mealType) AND " +
           "(:difficulty IS NULL OR r.difficulty = :difficulty) AND " +
           "(:maxTime IS NULL OR r.totalTimeMinutes <= :maxTime)")
    Page<Recipe> searchRecipes(
            @Param("query") String query,
            @Param("cuisine") String cuisine,
            @Param("dietaryType") DietaryType dietaryType,
            @Param("mealType") MealType mealType,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("maxTime") Integer maxTime,
            Pageable pageable
    );

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.recipeIngredients ri LEFT JOIN FETCH ri.ingredient " +
           "WHERE r.isPublished = true AND LOWER(r.title) LIKE LOWER(CONCAT('%', :dishName, '%'))")
    List<Recipe> findByTitleContainingIgnoreCaseWithIngredients(@Param("dishName") String dishName);

    long countByCuisine(String cuisine);
}
