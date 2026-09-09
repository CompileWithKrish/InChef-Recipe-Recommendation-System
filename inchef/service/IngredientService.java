package com.inchef.service;

import com.inchef.dto.request.IngredientCreateRequest;
import com.inchef.dto.response.IngredientResponse;
import com.inchef.entity.Ingredient;
import com.inchef.enums.MeasurementUnit;
import com.inchef.exception.ConflictException;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.repository.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<IngredientResponse> getAllIngredients(String query, String category) {
        List<Ingredient> list;
        if (query != null && !query.isBlank()) {
            list = ingredientRepository.findByNameContainingIgnoreCase(query.trim());
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
