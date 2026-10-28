package com.inchef.recommendation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;
import com.inchef.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/by-ingredients")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendByIngredients(
            @Valid @RequestBody IngredientRecommendationRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        request.setUserId(currentUserId);
        List<RecommendationResponse> recommendations = recommendationService.recommendByIngredients(request);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations generated successfully", recommendations));
    }

    @PostMapping("/by-dish")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendByDish(
            @Valid @RequestBody DishRecommendationRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        request.setUserId(currentUserId);
        List<RecommendationResponse> recommendations = recommendationService.recommendByDish(request);
        return ResponseEntity.ok(ApiResponse.ok("Dish recommendations generated successfully", recommendations));
    }
}
