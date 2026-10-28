package com.inchef.recipeimage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

import com.inchef.recipe.Recipe;

class RecipeHtmlMetadataExtractorTest {

    private RecipeHtmlMetadataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RecipeHtmlMetadataExtractor(new ObjectMapper());
    }

    @Test
    @DisplayName("Extract image from JSON-LD Recipe structured data (string format)")
    void testJsonLdStringImage() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Paneer Butter Masala",
                    "image": "https://images.example.com/paneer_butter_masala.jpg"
                }
                </script>
            </head>
            <body><h1>Recipe</h1></body>
            </html>
            """;

        Optional<String> result = extractor.extractImageUrl(html, "https://example.com/recipes/paneer");
        assertTrue(result.isPresent());
        assertEquals("https://images.example.com/paneer_butter_masala.jpg", result.get());
    }

    @Test
    @DisplayName("Extract image from JSON-LD Recipe structured data (@graph and ImageObject format)")
    void testJsonLdGraphImageObject() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@graph": [
                        {
                            "@type": "WebSite",
                            "name": "Indian Kitchen"
                        },
                        {
                            "@type": "Recipe",
                            "name": "Dal Makhani",
                            "image": {
                                "@type": "ImageObject",
                                "url": "https://images.example.com/dal_makhani.png"
                            }
                        }
                    ]
                }
                </script>
            </head>
            <body></body>
            </html>
            """;

        Optional<String> result = extractor.extractImageUrl(html, "https://example.com/dal-makhani");
        assertTrue(result.isPresent());
        assertEquals("https://images.example.com/dal_makhani.png", result.get());
    }

    @Test
    @DisplayName("Extract image from Open Graph metadata (og:image)")
    void testOpenGraphImage() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="Authentic Biryani">
                <meta property="og:image" content="https://images.archanaskitchen.com/biryani.jpg">
            </head>
            <body></body>
            </html>
            """;

        Optional<String> result = extractor.extractImageUrl(html, "https://www.archanaskitchen.com/biryani");
        assertTrue(result.isPresent());
        assertEquals("https://images.archanaskitchen.com/biryani.jpg", result.get());
    }

    @Test
    @DisplayName("Extract image from Twitter Card metadata (twitter:image)")
    void testTwitterCardImage() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:image" content="https://images.example.com/samosa.webp">
            </head>
            <body></body>
            </html>
            """;

        Optional<String> result = extractor.extractImageUrl(html, "https://example.com/samosa");
        assertTrue(result.isPresent());
        assertEquals("https://images.example.com/samosa.webp", result.get());
    }

    @Test
    @DisplayName("Resolve relative URL against base URL")
    void testRelativeUrlResolution() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="/media/photos/curry.jpg">
            </head>
            <body></body>
            </html>
            """;

        Optional<String> result = extractor.extractImageUrl(html, "https://example.com/recipes/chicken-curry");
        assertTrue(result.isPresent());
        assertEquals("https://example.com/media/photos/curry.jpg", result.get());
    }

    @Test
    @DisplayName("Reject when extracted URL equals the webpage URL itself")
    void testWebpageUrlRejection() {
        String pageUrl = "https://example.com/recipe/123";
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="%s">
            </head>
            <body></body>
            </html>
            """, pageUrl);

        Optional<String> result = extractor.extractImageUrl(html, pageUrl);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Graceful handling when HTML has no image metadata")
    void testNoImageMetadata() {
        String html = "<html><head><title>Just Text</title></head><body><p>Hello</p></body></html>";
        Optional<String> result = extractor.extractImageUrl(html, "https://example.com/article");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Extract multiple JSON-LD image candidates from array of strings and objects")
    void testMultipleJsonLdCandidates() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Chicken Tikka",
                    "image": [
                        "https://example.com/small_thumb.jpg",
                        {
                            "@type": "ImageObject",
                            "url": "https://example.com/highres_dish.jpg",
                            "width": 1600,
                            "height": 1200
                        },
                        "https://example.com/another_view.png"
                    ]
                }
                </script>
            </head>
            <body></body>
            </html>
            """;

        var candidates = extractor.extractImageCandidates(html, "https://example.com/recipe/1");
        assertEquals(3, candidates.size());
        assertEquals("https://example.com/small_thumb.jpg", candidates.get(0).url());
        assertEquals(RecipeHtmlMetadataExtractor.CandidateSource.JSON_LD, candidates.get(0).source());
        assertEquals("https://example.com/highres_dish.jpg", candidates.get(1).url());
        assertEquals(1600, candidates.get(1).declaredWidth());
        assertEquals(1200, candidates.get(1).declaredHeight());
        assertEquals("https://example.com/another_view.png", candidates.get(2).url());
    }

    @Test
    @DisplayName("Extract candidates across JSON-LD, OpenGraph, Twitter, and deduplicate without changing meaning")
    void testCrossSourceExtractionAndDeduplication() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Chole Bhature",
                    "image": "https://example.com/shared_image.jpg"
                }
                </script>
                <meta property="og:image" content="https://example.com/shared_image.jpg">
                <meta property="og:image:url" content="https://example.com/og_hero.jpg">
                <meta name="twitter:image" content="https://example.com/shared_image.jpg">
                <meta name="twitter:image:src" content="https://example.com/twitter_card.jpg">
                <link rel="image_src" href="/media/relative_dish.jpg">
            </head>
            <body>
                <article>
                    <img class="hero-dish rounded-3xl" alt="Chole Bhature Recipe Dish" src="https://example.com/body_photo.jpg">
                </article>
            </body>
            </html>
            """;

        var candidates = extractor.extractImageCandidates(html, "https://example.com/recipes/chole", "Chole Bhature");
        // shared_image.jpg should appear only once (JSON_LD), plus og_hero, twitter_card, relative_dish, and body_photo
        assertEquals(5, candidates.size());
        assertEquals("https://example.com/shared_image.jpg", candidates.get(0).url());
        assertEquals(RecipeHtmlMetadataExtractor.CandidateSource.JSON_LD, candidates.get(0).source());

        assertTrue(candidates.stream().anyMatch(c -> c.url().equals("https://example.com/og_hero.jpg")));
        assertTrue(candidates.stream().anyMatch(c -> c.url().equals("https://example.com/twitter_card.jpg")));
        assertTrue(candidates.stream().anyMatch(c -> c.url().equals("https://example.com/media/relative_dish.jpg")));
        assertTrue(candidates.stream().anyMatch(c -> c.url().equals("https://example.com/body_photo.jpg")));
    }

    @Test
    @DisplayName("Unwrap Next.js image proxy URLs and reject logo/icon images")
    void testNextJsUnwrapAndLogoRejection() {
        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <header>
                    <img src="/_next/image?url=%2Flogo.png&w=128&q=75" alt="Site Logo">
                    <img src="/icons/search.png" alt="Search">
                </header>
                <main>
                    <img class="rounded-3xl" alt="Mutter Paneer Recipe"
                         src="/_next/image?url=https%3A%2F%2Fstorage.example.com%2Fmutter_paneer_large.jpg&w=1920&q=90">
                </main>
            </body>
            </html>
            """;

        var candidates = extractor.extractImageCandidates(html, "https://example.com/mutter-paneer", "Mutter Paneer");
        assertEquals(1, candidates.size());
        assertEquals("https://storage.example.com/mutter_paneer_large.jpg", candidates.get(0).url());
        assertEquals(RecipeHtmlMetadataExtractor.CandidateSource.HTML_HERO, candidates.get(0).source());
    }
}
