package com.inchef.recipeimage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.inchef.common.ImageQualityProber;
import com.inchef.common.SsrfValidator;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class RecipeImageResolutionServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private SsrfValidator ssrfValidator;

    @Mock
    private RecipeHtmlMetadataExtractor metadataExtractor;

    @Mock
    private ImageSearchProvider imageSearchProvider;

    @Mock
    private ImageQualityProber imageQualityProber;

    private RecipeImageResolutionService service;

    @BeforeEach
    void setUp() {
        service = new RecipeImageResolutionService(
                recipeRepository,
                ssrfValidator,
                metadataExtractor,
                imageSearchProvider,
                imageQualityProber
        );
    }

    @Test
    @DisplayName("Strategy A: Existing verified high-quality image URL is preserved without refetching webpage")
    void testStrategyA_ExistingValidImage() {
        Recipe recipe = Recipe.builder()
                .id(1L)
                .title("Palak Paneer")
                .sourceUrl("https://www.archanaskitchen.com/palak-paneer")
                .imageUrl("https://images.archanaskitchen.com/palak-paneer.jpg")
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(ssrfValidator.isSafeUrl("https://images.archanaskitchen.com/palak-paneer.jpg")).thenReturn(true);
        when(imageQualityProber.probe(eq("https://images.archanaskitchen.com/palak-paneer.jpg"), eq("EXISTING"), eq("Palak Paneer")))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://images.archanaskitchen.com/palak-paneer.jpg",
                        "EXISTING",
                        new ImageQualityProber.ImageDimensions(800, 600),
                        "image/jpeg",
                        85,
                        true,
                        "High resolution"
                ));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = service.resolveRecipeImage(1L, false);

        assertEquals("https://images.archanaskitchen.com/palak-paneer.jpg", res.getImageUrl());
        assertEquals(ImageStatus.RESOLVED, res.getImageStatus());
        assertEquals(ImageSourceType.WEBPAGE_METADATA, res.getImageSourceType());
        verifyNoInteractions(metadataExtractor);
        verifyNoInteractions(imageSearchProvider);
    }

    @Test
    @DisplayName("Strategy B: Select high-resolution candidate appearing after a thumbnail")
    void testStrategyB_HighResolutionCandidateAppearingAfterThumbnail() {
        String html = "<html>...recipe content...</html>";
        String pageUrl = "https://example.com/recipe/tikka";
        String recipeTitle = "Paneer Tikka";

        var thumbCandidate = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/thumbnail.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.JSON_LD,
                150, 150, "Thumb"
        );
        var heroCandidate = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/hero_full.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.JSON_LD,
                1600, 1200, "Hero"
        );

        when(metadataExtractor.extractImageCandidates(html, pageUrl, recipeTitle))
                .thenReturn(List.of(thumbCandidate, heroCandidate));

        when(imageQualityProber.probe(eq("https://example.com/thumbnail.jpg"), anyString(), eq(recipeTitle)))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/thumbnail.jpg", "JSON_LD",
                        new ImageQualityProber.ImageDimensions(150, 150), "image/jpeg", 25, true, "Thumbnail"
                ));
        when(imageQualityProber.probe(eq("https://example.com/hero_full.jpg"), anyString(), eq(recipeTitle)))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/hero_full.jpg", "JSON_LD",
                        new ImageQualityProber.ImageDimensions(1600, 1200), "image/jpeg", 95, true, "Crisp hero"
                ));

        Optional<ImageQualityProber.ProbedImage> best = service.selectBestCandidateFromHtml(html, pageUrl, recipeTitle);

        assertTrue(best.isPresent());
        assertEquals("https://example.com/hero_full.jpg", best.get().url());
        assertEquals(95, best.get().qualityScore());
        assertEquals(1600, best.get().dimensions().width());
        assertEquals(1200, best.get().dimensions().height());
    }

    @Test
    @DisplayName("Strategy B: Existing low-res thumbnail is upgraded when a high-resolution candidate is verified")
    void testStrategyB_ExistingThumbnailUpgradedWhenHighResCandidateVerified() throws Exception {
        Recipe recipe = Recipe.builder()
                .id(10L)
                .title("Karela Masala")
                .sourceUrl("https://example.com/karela")
                .imageUrl("https://example.com/thumb_karela.jpg")
                .imageStatus(ImageStatus.PENDING)
                .build();

        // Existing image is probed as low-quality thumbnail (score 30)
        when(ssrfValidator.isSafeUrl("https://example.com/thumb_karela.jpg")).thenReturn(true);
        when(imageQualityProber.probe(eq("https://example.com/thumb_karela.jpg"), eq("EXISTING"), eq("Karela Masala")))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/thumb_karela.jpg", "EXISTING",
                        new ImageQualityProber.ImageDimensions(140, 156), "image/jpeg", 30, true, "Thumbnail"
                ));

        // Subclass service to return test HTML without network I/O
        RecipeImageResolutionService testService = new RecipeImageResolutionService(
                recipeRepository, ssrfValidator, metadataExtractor, imageSearchProvider, imageQualityProber
        ) {
            @Override
            protected String fetchHtmlContent(String sourceUrl) throws IOException {
                return "<html>test content</html>";
            }
        };

        var candidate = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/full_karela.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.HTML_HERO,
                1600, 1786, "Karela Dish"
        );
        when(metadataExtractor.extractImageCandidates(anyString(), eq("https://example.com/karela"), eq("Karela Masala")))
                .thenReturn(List.of(candidate));
        when(imageQualityProber.probe(eq("https://example.com/full_karela.jpg"), anyString(), eq("Karela Masala")))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/full_karela.jpg", "HTML_HERO",
                        new ImageQualityProber.ImageDimensions(1600, 1786), "image/jpeg", 95, true, "Full size"
                ));

        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = testService.resolveRecipeImage(10L, false);

        assertEquals("https://example.com/full_karela.jpg", res.getImageUrl());
        assertEquals(ImageStatus.RESOLVED, res.getImageStatus());
        assertEquals(ImageSourceType.WEBPAGE_METADATA, res.getImageSourceType());
    }

    @Test
    @DisplayName("Strategy B: Existing image remains unchanged when candidates on webpage are lower quality")
    void testStrategyB_ExistingImagePreservedWhenCandidateIsLowerQuality() throws Exception {
        Recipe recipe = Recipe.builder()
                .id(11L)
                .title("Biryani Special")
                .sourceUrl("https://example.com/biryani")
                .imageUrl("https://example.com/existing_biryani.jpg")
                .imageStatus(ImageStatus.PENDING)
                .build();

        // Existing image probed with good score 70
        when(ssrfValidator.isSafeUrl("https://example.com/existing_biryani.jpg")).thenReturn(true);
        when(imageQualityProber.probe(eq("https://example.com/existing_biryani.jpg"), eq("EXISTING"), eq("Biryani Special")))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/existing_biryani.jpg", "EXISTING",
                        new ImageQualityProber.ImageDimensions(600, 400), "image/jpeg", 70, true, "Good quality"
                ));

        RecipeImageResolutionService testService = new RecipeImageResolutionService(
                recipeRepository, ssrfValidator, metadataExtractor, imageSearchProvider, imageQualityProber
        ) {
            @Override
            protected String fetchHtmlContent(String sourceUrl) throws IOException {
                return "<html>test content</html>";
            }
        };

        // Source page only contains a tiny 100x100 thumbnail (score 20)
        var thumb = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/tiny_thumb.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.OPEN_GRAPH,
                100, 100, "Thumb"
        );
        when(metadataExtractor.extractImageCandidates(anyString(), eq("https://example.com/biryani"), eq("Biryani Special")))
                .thenReturn(List.of(thumb));
        when(imageQualityProber.probe(eq("https://example.com/tiny_thumb.jpg"), anyString(), eq("Biryani Special")))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/tiny_thumb.jpg", "OPEN_GRAPH",
                        new ImageQualityProber.ImageDimensions(100, 100), "image/jpeg", 20, true, "Tiny thumb"
                ));

        when(recipeRepository.findById(11L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = testService.resolveRecipeImage(11L, false);

        // Crucial: Must NOT overwrite with the lower quality candidate!
        assertEquals("https://example.com/existing_biryani.jpg", res.getImageUrl());
        assertEquals(ImageStatus.RESOLVED, res.getImageStatus());
    }

    @Test
    @DisplayName("Strategy B: Open Graph and Twitter Card fallbacks when no JSON-LD is available")
    void testStrategyB_OpenGraphAndTwitterFallback() {
        String html = "<html>metadata fallback</html>";
        String pageUrl = "https://example.com/recipes/dosa";
        String title = "Masala Dosa";

        var ogCandidate = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/og_dosa.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.OPEN_GRAPH,
                1200, 630, "OG Dosa"
        );
        var twitterCandidate = new RecipeHtmlMetadataExtractor.ImageCandidate(
                "https://example.com/twitter_dosa.jpg",
                RecipeHtmlMetadataExtractor.CandidateSource.TWITTER_CARD,
                800, 418, "Twitter Dosa"
        );

        when(metadataExtractor.extractImageCandidates(html, pageUrl, title))
                .thenReturn(List.of(ogCandidate, twitterCandidate));

        when(imageQualityProber.probe(eq("https://example.com/og_dosa.jpg"), anyString(), eq(title)))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/og_dosa.jpg", "OPEN_GRAPH",
                        new ImageQualityProber.ImageDimensions(1200, 630), "image/jpeg", 85, true, "Crisp OG"
                ));
        when(imageQualityProber.probe(eq("https://example.com/twitter_dosa.jpg"), anyString(), eq(title)))
                .thenReturn(new ImageQualityProber.ProbedImage(
                        "https://example.com/twitter_dosa.jpg", "TWITTER_CARD",
                        new ImageQualityProber.ImageDimensions(800, 418), "image/jpeg", 75, true, "Twitter card"
                ));

        Optional<ImageQualityProber.ProbedImage> selected = service.selectBestCandidateFromHtml(html, pageUrl, title);

        assertTrue(selected.isPresent());
        assertEquals("https://example.com/og_dosa.jpg", selected.get().url());
        assertEquals(85, selected.get().qualityScore());
    }

    @Test
    @DisplayName("SSRF protection: Blocks private network source URL and falls back gracefully")
    void testSSRFProtection_BlocksRestrictedSourceUrl() {
        Recipe recipe = Recipe.builder()
                .id(15L)
                .title("Internal Secret Dish")
                .sourceUrl("http://169.254.169.254/latest/meta-data")
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(ssrfValidator.isSafeUrl("http://169.254.169.254/latest/meta-data")).thenReturn(false);
        when(imageSearchProvider.isAvailable()).thenReturn(false);

        when(recipeRepository.findById(15L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = service.resolveRecipeImage(15L, false);

        assertEquals(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER, res.getImageUrl());
        assertEquals(ImageStatus.PLACEHOLDER, res.getImageStatus());
        verifyNoInteractions(metadataExtractor);
    }

    @Test
    @DisplayName("External request failure: Graceful fallback to placeholder without throwing exceptions")
    void testExternalRequestFailure_GracefulFallbackToPlaceholder() {
        Recipe recipe = Recipe.builder()
                .id(16L)
                .title("Failing Dish")
                .sourceUrl("https://example.com/broken-page")
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(ssrfValidator.isSafeUrl("https://example.com/broken-page")).thenReturn(true);
        when(imageSearchProvider.isAvailable()).thenReturn(false);

        when(recipeRepository.findById(16L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        // Network connection to broken-page will fail (e.g. UnknownHost or refused)
        assertDoesNotThrow(() -> {
            RecipeImageResponse res = service.resolveRecipeImage(16L, false);
            assertEquals(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER, res.getImageUrl());
            assertEquals(ImageStatus.PLACEHOLDER, res.getImageStatus());
        });
    }

    @Test
    @DisplayName("Strategy C: Fallback to image search provider when source webpage has no image")
    void testStrategyC_SearchFallback() {
        Recipe recipe = Recipe.builder()
                .id(2L)
                .title("Kashmiri Rogan Josh")
                .cuisine("Kashmiri")
                .sourceUrl(null)
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(imageSearchProvider.isAvailable()).thenReturn(true);
        when(imageSearchProvider.getProviderName()).thenReturn("GoogleCustomSearch");
        when(imageSearchProvider.searchImage(eq("Kashmiri Rogan Josh"), eq("Kashmiri"), anyList()))
                .thenReturn(Optional.of("https://images.example.com/rogan-josh.jpg"));

        when(recipeRepository.findById(2L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = service.resolveRecipeImage(2L, false);

        assertEquals("https://images.example.com/rogan-josh.jpg", res.getImageUrl());
        assertEquals(ImageStatus.RESOLVED, res.getImageStatus());
        assertEquals(ImageSourceType.IMAGE_SEARCH, res.getImageSourceType());
    }

    @Test
    @DisplayName("Strategy D: Neutral placeholder assigned when all external sources fail")
    void testStrategyD_PlaceholderFallback() {
        Recipe recipe = Recipe.builder()
                .id(3L)
                .title("Mystery Dish")
                .sourceUrl(null)
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(imageSearchProvider.isAvailable()).thenReturn(false);
        when(recipeRepository.findById(3L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeImageResponse res = service.resolveRecipeImage(3L, false);

        assertEquals(RecipeImageResolutionService.NEUTRAL_PLACEHOLDER, res.getImageUrl());
        assertEquals(ImageStatus.PLACEHOLDER, res.getImageStatus());
        assertEquals(ImageSourceType.PLACEHOLDER, res.getImageSourceType());
    }

    @Test
    @DisplayName("Batch execution respects dryRun mode without updating database")
    void testBatchExecution_DryRun() {
        Recipe recipe = Recipe.builder()
                .id(4L)
                .title("Vegetable Pulao")
                .sourceUrl(null)
                .imageStatus(ImageStatus.PENDING)
                .build();

        when(recipeRepository.findByImageStatusIn(anyList(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(imageSearchProvider.isAvailable()).thenReturn(false);

        RecipeImageBatchStatus status = service.executeBatchResolution(10, 5, true, false);

        assertEquals(1, status.getTotalProcessed());
        assertEquals(1, status.getPlaceholderAssignedCount());
        assertTrue(status.isDryRun());
        verify(recipeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Audit report aggregates database statistics correctly")
    void testAuditReport() {
        when(recipeRepository.count()).thenReturn(6865L);
        when(recipeRepository.countByImageStatus(ImageStatus.RESOLVED)).thenReturn(501L);
        when(recipeRepository.countByImageStatus(ImageStatus.PENDING)).thenReturn(6364L);
        when(recipeRepository.countByImageStatus(ImageStatus.FAILED)).thenReturn(0L);
        when(recipeRepository.countByImageStatus(ImageStatus.PLACEHOLDER)).thenReturn(0L);
        when(recipeRepository.countDistinctValidImages()).thenReturn(501L);
        when(recipeRepository.findRepeatedImages()).thenReturn(Collections.emptyList());

        RecipeImageAuditReport report = service.generateAuditReport();

        assertEquals(6865L, report.getTotalRecipes());
        assertEquals(501L, report.getExistingImagesReused());
        assertEquals(6364L, report.getPendingCount());
        assertEquals(501L, report.getUniqueNonemptyImageCount());
        assertEquals(0L, report.getRepeatedImageCount());
        assertNotNull(report.getGeneratedAt());
    }
}
