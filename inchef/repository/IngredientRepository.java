package com.inchef.repository;

import com.inchef.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);
    List<Ingredient> findByNameInIgnoreCase(Collection<String> names);
    List<Ingredient> findByCategoryIgnoreCase(String category);
    List<Ingredient> findByNameContainingIgnoreCase(String query);
    boolean existsByNameIgnoreCase(String name);
}
