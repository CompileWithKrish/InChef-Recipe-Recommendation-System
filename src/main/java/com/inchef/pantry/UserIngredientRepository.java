package com.inchef.pantry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    @Query("SELECT ui FROM UserIngredient ui JOIN FETCH ui.ingredient WHERE ui.user.id = :userId")
    List<UserIngredient> findByUserIdWithIngredient(@Param("userId") Long userId);

    List<UserIngredient> findByUserId(Long userId);

    Optional<UserIngredient> findByUserIdAndIngredientId(Long userId, Long ingredientId);

    void deleteByUserIdAndIngredientId(Long userId, Long ingredientId);

    void deleteByUserId(Long userId);
}
