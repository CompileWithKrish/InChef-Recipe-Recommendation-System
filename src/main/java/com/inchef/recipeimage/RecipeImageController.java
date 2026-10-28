package com.inchef.recipeimage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import java.net.URI;
import java.util.Optional;

import com.inchef.common.ApiResponse;
import com.inchef.exception.UnauthorizedException;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;
import com.inchef.security.SecurityUtils;

@RestController
public class RecipeImageController {

    private final RecipeImageResolutionService imageResolutionService;
    private final RecipeRepository recipeRepository;

    @Value("${inchef.admin.secret:InChefSecureAdminKey2026}")
    private String adminSecret;

    public RecipeImageController(RecipeImageResolutionService imageResolutionService) {
        this(imageResolutionService, null);
    }

    @Autowired
    public RecipeImageController(RecipeImageResolutionService imageResolutionService, RecipeRepository recipeRepository) {
        this.imageResolutionService = imageResolutionService;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Public endpoint to view a recipe's verified image and metadata.
     * GET /api/v1/recipes/{id}/image
     */
    @GetMapping("/api/v1/recipes/{id:\\d+}/image")
    public ResponseEntity<ApiResponse<RecipeImageResponse>> getRecipeImageMetadata(@PathVariable Long id) {
        RecipeImageResponse metadata = imageResolutionService.getRecipeImageMetadata(id);
        return ResponseEntity.ok(ApiResponse.ok(metadata));
    }

    /**
     * Public image proxy endpoint that serves verified recipe image bytes.
     * Prevents third-party CDN hotlink 403 Forbidden errors in user browsers.
     * GET /api/v1/recipes/{id}/image/content
     */
    @GetMapping("/api/v1/recipes/{id:\\d+}/image/content")
    public ResponseEntity<byte[]> getRecipeImageContent(@PathVariable Long id) {
        Recipe recipe = recipeRepository.findById(id).orElse(null);
        if (recipe == null || recipe.getImageUrl() == null || recipe.getImageUrl().isBlank()
                || recipe.getImageUrl().equals(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER))
                    .build();
        }

        Optional<RecipeImageResolutionService.ProxiedImage> proxied = imageResolutionService.proxyImage(recipe.getImageUrl());
        if (proxied.isPresent()) {
            RecipeImageResolutionService.ProxiedImage img = proxied.get();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, img.getContentType())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(img.getData());
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER))
                .build();
    }

    /**
     * Admin endpoint to trigger a controlled batch resolution process.
     * POST /api/v1/admin/recipe-images/resolve
     */
    @PostMapping("/api/v1/admin/recipe-images/resolve")
    public ResponseEntity<ApiResponse<RecipeImageBatchStatus>> resolveRecipeImagesBatch(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "25") int batchSize,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestHeader(value = "X-Admin-Key", required = false) String providedAdminKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        validateAdminAccess(providedAdminKey, authHeader);

        RecipeImageBatchStatus result = imageResolutionService.executeBatchResolution(limit, batchSize, dryRun, force);
        return ResponseEntity.ok(ApiResponse.ok("Batch resolution executed successfully", result));
    }

    /**
     * Admin endpoint to resolve an individual recipe on demand.
     * POST /api/v1/admin/recipe-images/resolve/{id}
     */
    @PostMapping("/api/v1/admin/recipe-images/resolve/{id:\\d+}")
    public ResponseEntity<ApiResponse<RecipeImageResponse>> resolveSingleRecipeImage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestHeader(value = "X-Admin-Key", required = false) String providedAdminKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        validateAdminAccess(providedAdminKey, authHeader);

        RecipeImageResponse response = imageResolutionService.resolveRecipeImage(id, force);
        return ResponseEntity.ok(ApiResponse.ok("Recipe image resolved successfully", response));
    }

    /**
     * Admin endpoint to check batch execution status.
     * GET /api/v1/admin/recipe-images/status
     */
    @GetMapping("/api/v1/admin/recipe-images/status")
    public ResponseEntity<ApiResponse<RecipeImageBatchStatus>> getBatchStatus(
            @RequestHeader(value = "X-Admin-Key", required = false) String providedAdminKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        validateAdminAccess(providedAdminKey, authHeader);

        RecipeImageBatchStatus status = imageResolutionService.getCurrentBatchStatus();
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * Admin endpoint to generate a quantitative audit report of all recipe images.
     * GET /api/v1/admin/recipe-images/audit
     */
    @GetMapping("/api/v1/admin/recipe-images/audit")
    public ResponseEntity<ApiResponse<RecipeImageAuditReport>> getAuditReport(
            @RequestHeader(value = "X-Admin-Key", required = false) String providedAdminKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        validateAdminAccess(providedAdminKey, authHeader);

        RecipeImageAuditReport report = imageResolutionService.generateAuditReport();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    private void validateAdminAccess(String providedKey, String authHeader) {
        // 1. Check if authenticated user principal exists (e.g. from valid JWT session)
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            return;
        }

        String configuredSecret = (adminSecret != null) ? adminSecret.trim() : "";
        if (configuredSecret.isEmpty()) {
            throw new UnauthorizedException("Administrative secret is not configured on the server.");
        }

        byte[] expectedBytes = configuredSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 2. Validate X-Admin-Key header (constant-time comparison)
        if (providedKey != null && !providedKey.isBlank()) {
            byte[] providedBytes = providedKey.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (java.security.MessageDigest.isEqual(providedBytes, expectedBytes)) {
                return;
            }
        }

        // 3. Validate Authorization header (Bearer <secret> or raw <secret>)
        if (authHeader != null && !authHeader.isBlank()) {
            String token = authHeader.trim();
            if (token.toLowerCase().startsWith("bearer ")) {
                token = token.substring(7).trim();
            }
            byte[] tokenBytes = token.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (java.security.MessageDigest.isEqual(tokenBytes, expectedBytes)) {
                return;
            }
        }

        throw new UnauthorizedException(
                "Administrative authorization required. Provide valid 'X-Admin-Key' header or 'Authorization: Bearer <admin_key>'.");
    }
}
