package com.inchef.preference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.inchef.user.User;
import com.inchef.user.UserService;

@Service
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserService userService;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository, UserService userService) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getUserPreference(Long userId) {
        userService.findEntityById(userId);
        return userPreferenceRepository.findByUserId(userId)
                .map(this::mapToResponse)
                .orElse(UserPreferenceResponse.builder()
                        .userId(userId)
                        .preferredCuisines(Collections.emptyList())
                        .allergies(Collections.emptyList())
                        .build());
    }

    public UserPreferenceResponse saveOrUpdateUserPreference(Long userId, UserPreferenceRequest request) {
        User user = userService.findEntityById(userId);

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder().user(user).build());

        preference.setDietaryType(request.getDietaryType());
        preference.setMaxCookingTime(request.getMaxCookingTime());
        preference.setPreferredSpiceLevel(request.getPreferredSpiceLevel());
        if (request.getCookingSkillLevel() != null) {
            preference.setCookingSkillLevel(request.getCookingSkillLevel());
        }

        if (request.getPreferredCuisines() != null) {
            List<String> validCuisines = request.getPreferredCuisines().stream()
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .map(String::trim)
                    .toList();
            preference.setPreferredCuisines(validCuisines.isEmpty() ? null : String.join(",", validCuisines));
        } else {
            preference.setPreferredCuisines(null);
        }
        if (request.getAllergies() != null) {
            List<String> validAllergies = request.getAllergies().stream()
                    .filter(a -> a != null && !a.trim().isEmpty())
                    .map(String::trim)
                    .toList();
            preference.setAllergies(validAllergies.isEmpty() ? null : String.join(",", validAllergies));
        } else {
            preference.setAllergies(null);
        }

        UserPreference saved = userPreferenceRepository.save(preference);
        return mapToResponse(saved);
    }

    private UserPreferenceResponse mapToResponse(UserPreference pref) {
        List<String> cuisines = pref.getPreferredCuisines() != null && !pref.getPreferredCuisines().isBlank()
                ? Arrays.stream(pref.getPreferredCuisines().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        List<String> allergies = pref.getAllergies() != null && !pref.getAllergies().isBlank()
                ? Arrays.stream(pref.getAllergies().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return UserPreferenceResponse.builder()
                .id(pref.getId())
                .userId(pref.getUser().getId())
                .dietaryType(pref.getDietaryType())
                .maxCookingTime(pref.getMaxCookingTime())
                .preferredCuisines(cuisines)
                .allergies(allergies)
                .preferredSpiceLevel(pref.getPreferredSpiceLevel())
                .cookingSkillLevel(pref.getCookingSkillLevel())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }
}
