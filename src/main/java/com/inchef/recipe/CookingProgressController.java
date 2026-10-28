package com.inchef.recipe;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.inchef.common.ApiResponse;
import com.inchef.security.UserPrincipal;

@RestController
@RequestMapping("/api/v1/recipes/{recipeId}")
public class CookingProgressController {

    private final CookingProgressService progressService;
    private final RecipeService recipeService;

    public CookingProgressController(CookingProgressService progressService, RecipeService recipeService) {
        this.progressService = progressService;
        this.recipeService = recipeService;
    }

    /**
     * Get structured cooking instruction steps without modifying stored recipe text.
     */
    @GetMapping("/instructions")
    public ResponseEntity<ApiResponse<List<String>>> getInstructionSteps(@PathVariable Long recipeId) {
        Recipe recipe = recipeService.findEntityById(recipeId);
        List<String> steps = progressService.parseInstructionSteps(recipe.getInstructions());
        return ResponseEntity.ok(ApiResponse.ok("Recipe cooking steps retrieved", steps));
    }

    /**
     * Get user cooking progress for a recipe.
     */
    @GetMapping("/cooking-progress")
    public ResponseEntity<ApiResponse<CookingProgressResponse>> getProgress(
            @PathVariable Long recipeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer totalSteps,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long effectiveUserId = (principal != null) ? principal.getId() : userId;

        int safeTotal = totalSteps;
        if (safeTotal <= 0) {
            Recipe recipe = recipeService.findEntityById(recipeId);
            safeTotal = progressService.parseInstructionSteps(recipe.getInstructions()).size();
        }

        CookingProgressResponse response = progressService.getProgress(effectiveUserId, recipeId, safeTotal);
        return ResponseEntity.ok(ApiResponse.ok("Cooking progress retrieved", response));
    }

    /**
     * Update cooking progress (toggle individual step or update entire completed list).
     */
    @PostMapping("/cooking-progress")
    public ResponseEntity<ApiResponse<CookingProgressResponse>> updateProgress(
            @PathVariable Long recipeId,
            @RequestBody CookingProgressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long effectiveUserId = (principal != null) ? principal.getId() : request.getUserId();

        int totalSteps = (request.getTotalSteps() != null && request.getTotalSteps() > 0)
                ? request.getTotalSteps()
                : progressService.parseInstructionSteps(recipeService.findEntityById(recipeId).getInstructions()).size();

        CookingProgressResponse response;
        if (request.getCompletedStepIndices() != null) {
            response = progressService.setAllSteps(effectiveUserId, recipeId, request.getCompletedStepIndices(), totalSteps);
        } else if (request.getStepIndex() != null && request.getCompleted() != null) {
            response = progressService.updateStep(effectiveUserId, recipeId, request.getStepIndex(), request.getCompleted(), totalSteps);
        } else {
            response = progressService.getProgress(effectiveUserId, recipeId, totalSteps);
        }

        return ResponseEntity.ok(ApiResponse.ok("Cooking progress updated", response));
    }

    /**
     * Reset cooking progress for a recipe.
     */
    @PostMapping("/cooking-progress/reset")
    public ResponseEntity<ApiResponse<CookingProgressResponse>> resetProgress(
            @PathVariable Long recipeId,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long effectiveUserId = (principal != null) ? principal.getId() : userId;
        int totalSteps = progressService.parseInstructionSteps(recipeService.findEntityById(recipeId).getInstructions()).size();
        CookingProgressResponse response = progressService.resetProgress(effectiveUserId, recipeId, totalSteps);
        return ResponseEntity.ok(ApiResponse.ok("Cooking progress reset successfully", response));
    }
}
