package com.inchef.service;

import com.inchef.dto.request.UserIngredientRequest;
import com.inchef.dto.response.UserIngredientResponse;
import com.inchef.entity.Ingredient;
import com.inchef.entity.User;
import com.inchef.entity.UserIngredient;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.repository.UserIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserIngredientService {

    private final UserIngredientRepository userIngredientRepository;
    private final UserService userService;
    private final IngredientService ingredientService;

    public UserIngredientService(UserIngredientRepository userIngredientRepository,
                                 UserService userService,
                                 IngredientService ingredientService) {
        this.userIngredientRepository = userIngredientRepository;
        this.userService = userService;
        this.ingredientService = ingredientService;
    }

    @Transactional(readOnly = true)
    public List<UserIngredientResponse> getUserIngredients(Long userId) {
        userService.findEntityById(userId); // ensure user exists
        return userIngredientRepository.findByUserIdWithIngredient(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserIngredientResponse addOrUpdateUserIngredient(Long userId, UserIngredientRequest request) {
        User user = userService.findEntityById(userId);

        Ingredient ingredient;
        if (request.getIngredientId() != null) {
            ingredient = ingredientService.findEntityById(request.getIngredientId());
        } else if (request.getIngredientName() != null && !request.getIngredientName().isBlank()) {
            ingredient = ingredientService.getOrCreateIngredient(
                    request.getIngredientName(),
                    request.getCategory() != null ? request.getCategory() : "General",
                    request.getUnit()
            );
        } else {
            throw new IllegalArgumentException("Either ingredientId or ingredientName must be specified");
        }

        Optional<UserIngredient> existing = userIngredientRepository.findByUserIdAndIngredientId(userId, ingredient.getId());
        UserIngredient userIngredient;
        if (existing.isPresent()) {
            userIngredient = existing.get();
            userIngredient.setQuantity(request.getQuantity());
            userIngredient.setUnit(request.getUnit());
            userIngredient.setExpiryDate(request.getExpiryDate());
        } else {
            userIngredient = UserIngredient.builder()
                    .user(user)
                    .ingredient(ingredient)
                    .quantity(request.getQuantity())
                    .unit(request.getUnit())
                    .expiryDate(request.getExpiryDate())
                    .build();
        }

        UserIngredient saved = userIngredientRepository.save(userIngredient);
        return mapToResponse(saved);
    }

    public void removeUserIngredient(Long userId, Long ingredientId) {
        userService.findEntityById(userId);
        UserIngredient ui = userIngredientRepository.findByUserIdAndIngredientId(userId, ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("UserIngredient", "ingredientId", ingredientId));
        userIngredientRepository.delete(ui);
    }

    public void clearUserIngredients(Long userId) {
        userService.findEntityById(userId);
        userIngredientRepository.deleteByUserId(userId);
    }

    private UserIngredientResponse mapToResponse(UserIngredient ui) {
        return UserIngredientResponse.builder()
                .id(ui.getId())
                .ingredientId(ui.getIngredient().getId())
                .ingredientName(ui.getIngredient().getName())
                .category(ui.getIngredient().getCategory())
                .quantity(ui.getQuantity())
                .unit(ui.getUnit())
                .expiryDate(ui.getExpiryDate())
                .imageUrl(ui.getIngredient().getImageUrl())
                .addedAt(ui.getAddedAt())
                .build();
    }
}
