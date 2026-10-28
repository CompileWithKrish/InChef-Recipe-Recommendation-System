package com.inchef.ingredient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.inchef.common.MeasurementUnit;
import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;

@Service
@Transactional
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public IngredientResponse createIngredient(IngredientCreateRequest request) {
        if (ingredientRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ConflictException("Ingredient '" + request.getName() + "' already exists");
        }

        Ingredient ingredient = Ingredient.builder()
                .name(request.getName().trim())
                .category(request.getCategory().trim())
                .defaultUnit(request.getDefaultUnit())
                .imageUrl(request.getImageUrl())
                .build();

        return mapToResponse(ingredientRepository.save(ingredient));
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredientById(Long id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> autocompleteIngredients(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        int maxLimit = Math.min(Math.max(limit, 1), 50);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, maxLimit);
        List<Ingredient> list = ingredientRepository.searchForAutocomplete(query.trim(), pageable);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients(String query, String category) {
        return getAllIngredients(query, category, null);
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients(String query, String category, Integer limit) {
        List<Ingredient> list;
        if (query != null && !query.isBlank()) {
            if (limit != null && limit > 0) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 100));
                list = ingredientRepository.searchForAutocomplete(query.trim(), pageable);
            } else {
                list = ingredientRepository.findByNameContainingIgnoreCase(query.trim());
            }
        } else if (category != null && !category.isBlank()) {
            list = ingredientRepository.findByCategoryIgnoreCase(category.trim());
        } else {
            list = ingredientRepository.findAll();
        }

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public Ingredient getOrCreateIngredient(String name, String category, MeasurementUnit defaultUnit) {
        return ingredientRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> {
                    Ingredient newIng = Ingredient.builder()
                            .name(name.trim())
                            .category(category != null ? category.trim() : "Other")
                            .defaultUnit(defaultUnit != null ? defaultUnit : MeasurementUnit.PIECES)
                            .build();
                    return ingredientRepository.save(newIng);
                });
    }

    public void deleteIngredient(Long id) {
        Ingredient ingredient = findEntityById(id);
        ingredientRepository.delete(ingredient);
    }

    @Transactional(readOnly = true)
    public Ingredient findEntityById(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient", "id", id));
    }

    public IngredientResponse mapToResponse(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .category(ingredient.getCategory())
                .defaultUnit(ingredient.getDefaultUnit())
                .imageUrl(ingredient.getImageUrl())
                .createdAt(ingredient.getCreatedAt())
                .build();
    }
}
