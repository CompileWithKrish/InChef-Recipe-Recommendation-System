package com.inchef.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @Query("SELECT f FROM Favorite f JOIN FETCH f.recipe r WHERE f.user.id = :userId ORDER BY f.savedAt DESC")
    List<Favorite> findByUserIdWithRecipe(@Param("userId") Long userId);

    Optional<Favorite> findByUserIdAndRecipeId(Long userId, Long recipeId);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);

    long countByUserId(Long userId);

    long countByRecipeId(Long recipeId);
}
