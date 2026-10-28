package com.inchef.recipeimage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.inchef.exception.UnauthorizedException;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class RecipeImageControllerTest {

    @Mock
    private RecipeImageResolutionService imageResolutionService;

    @Mock
    private RecipeRepository recipeRepository;

    private RecipeImageController controller;
    private final String validSecret = "TestAdminSecretKey123";

    @BeforeEach
    void setUp() {
        controller = new RecipeImageController(imageResolutionService, recipeRepository);
        ReflectionTestUtils.setField(controller, "adminSecret", validSecret);
    }

    @Test
    @DisplayName("Public endpoint /api/v1/recipes/{id}/image succeeds without credentials")
    void testPublicImageMetadata_NoAuthRequired() {
        RecipeImageResponse mockResp = new RecipeImageResponse(
                1L, "Paneer Tikka", "IFD-1", "https://example.com/recipe",
                "https://images.example.com/paneer.jpg", "https://example.com/recipe",
                ImageSourceType.WEBPAGE_METADATA, ImageStatus.RESOLVED, null, null
        );
        when(imageResolutionService.getRecipeImageMetadata(1L)).thenReturn(mockResp);

        ResponseEntity<?> response = controller.getRecipeImageMetadata(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Admin audit endpoint rejects missing credentials with 401 Unauthorized")
    void testAudit_MissingCredentials() {
        assertThrows(UnauthorizedException.class, () -> {
            controller.getAuditReport(null, null);
        });
    }

    @Test
    @DisplayName("Admin audit endpoint rejects invalid X-Admin-Key with 401 Unauthorized")
    void testAudit_InvalidXAdminKey() {
        assertThrows(UnauthorizedException.class, () -> {
            controller.getAuditReport("WrongSecretKey", null);
        });
    }

    @Test
    @DisplayName("Admin audit endpoint accepts valid X-Admin-Key")
    void testAudit_ValidXAdminKey() {
        when(imageResolutionService.generateAuditReport()).thenReturn(new RecipeImageAuditReport());

        ResponseEntity<?> response = controller.getAuditReport(validSecret, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(imageResolutionService).generateAuditReport();
    }

    @Test
    @DisplayName("Admin audit endpoint accepts valid Authorization: Bearer <key>")
    void testAudit_ValidAuthorizationBearerHeader() {
        when(imageResolutionService.generateAuditReport()).thenReturn(new RecipeImageAuditReport());

        ResponseEntity<?> response = controller.getAuditReport(null, "Bearer " + validSecret);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(imageResolutionService).generateAuditReport();
    }

    @Test
    @DisplayName("Admin audit endpoint accepts valid Authorization: <key> without Bearer prefix")
    void testAudit_ValidAuthorizationRawHeader() {
        when(imageResolutionService.generateAuditReport()).thenReturn(new RecipeImageAuditReport());

        ResponseEntity<?> response = controller.getAuditReport(null, validSecret);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(imageResolutionService).generateAuditReport();
    }

    @Test
    @DisplayName("Admin audit endpoint rejects invalid Authorization header with 401 Unauthorized")
    void testAudit_InvalidAuthorizationHeader() {
        assertThrows(UnauthorizedException.class, () -> {
            controller.getAuditReport(null, "Bearer completely-wrong-token");
        });
    }

    @Test
    @DisplayName("Public proxy endpoint /api/v1/recipes/{id}/image/content returns image bytes")
    void testProxyImageContent_Success() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setImageUrl("https://images.example.com/paneer.jpg");
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

        RecipeImageResolutionService.ProxiedImage mockImg = 
                new RecipeImageResolutionService.ProxiedImage(new byte[]{1, 2, 3}, "image/jpeg");
        when(imageResolutionService.proxyImage("https://images.example.com/paneer.jpg")).thenReturn(Optional.of(mockImg));

        ResponseEntity<byte[]> response = controller.getRecipeImageContent(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
    }
}
