package com.inchef.controller;

import com.inchef.dto.request.DishRecommendationRequest;
import com.inchef.dto.request.IngredientRecommendationRequest;
import com.inchef.dto.response.ApiResponse;
import com.inchef.dto.response.RecommendationResponse;
import com.inchef.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        List<RecommendationResponse> recommendations = recommendationService.recommendByIngredients(request);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations generated successfully", recommendations));
    }

    @PostMapping("/by-dish")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendByDish(
            @Valid @RequestBody DishRecommendationRequest request) {
        List<RecommendationResponse> recommendations = recommendationService.recommendByDish(request);
        return ResponseEntity.ok(ApiResponse.ok("Dish recommendations generated successfully", recommendations));
    }
}
