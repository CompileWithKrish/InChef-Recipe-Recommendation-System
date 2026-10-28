package com.inchef.ingredient;

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

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Ingredient i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY CASE WHEN LOWER(i.name) LIKE LOWER(CONCAT(:query, '%')) THEN 0 ELSE 1 END, " +
           "LENGTH(i.name) ASC, i.name ASC")
    List<Ingredient> searchForAutocomplete(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);
}
