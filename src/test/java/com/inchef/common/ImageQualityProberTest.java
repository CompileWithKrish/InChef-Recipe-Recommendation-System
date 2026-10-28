package com.inchef.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageQualityProberTest {

    @Mock
    private SsrfValidator ssrfValidator;

    private ImageQualityProber prober;

    @BeforeEach
    void setUp() {
        prober = new ImageQualityProber(ssrfValidator);
    }

    @Test
    @DisplayName("SSRF protection: rejects unsafe URL before making network calls")
    void testSsrfProtectionRejection() {
        when(ssrfValidator.isSafeUrl("http://169.254.169.254/latest/meta-data")).thenReturn(false);

        ImageQualityProber.ProbedImage result = prober.probe(
                "http://169.254.169.254/latest/meta-data", "TEST", "Some Recipe");

        assertFalse(result.accessible());
        assertEquals(-100, result.qualityScore());
        assertTrue(result.reason().contains("SSRF"));
    }

    @Test
    @DisplayName("Path filtering: rejects logo, avatar, and placeholder URLs")
    void testLogoAndPlaceholderRejection() {
        when(ssrfValidator.isSafeUrl(anyString())).thenReturn(true);

        ImageQualityProber.ProbedImage logo = prober.probe(
                "https://example.com/images/logos/main_logo.png", "TEST", "Recipe");
        assertFalse(logo.accessible());
        assertTrue(logo.reason().contains("Logo"));

        ImageQualityProber.ProbedImage avatar = prober.probe(
                "https://example.com/static/avatars/user_1.jpg", "TEST", "Recipe");
        assertFalse(avatar.accessible());

        ImageQualityProber.ProbedImage search = prober.probe(
                "https://example.com/icons/search.png", "TEST", "Recipe");
        assertFalse(search.accessible());
    }

    @Test
    @DisplayName("Dimension extraction: accurately extracts width and height from JPEG bytes")
    void testJpegDimensionExtraction() throws Exception {
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 1280, 720);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] jpegBytes = baos.toByteArray();

        ImageQualityProber.ImageDimensions dims = prober.extractDimensions(jpegBytes, "image/jpeg");
        assertNotNull(dims);
        assertEquals(1280, dims.width());
        assertEquals(720, dims.height());
        assertEquals(1280 * 720, dims.getArea());
    }

    @Test
    @DisplayName("Dimension extraction: accurately extracts width and height from PNG bytes")
    void testPngDimensionExtraction() throws Exception {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        ImageQualityProber.ImageDimensions dims = prober.extractDimensions(pngBytes, "image/png");
        assertNotNull(dims);
        assertEquals(800, dims.width());
        assertEquals(600, dims.height());
    }

    @Test
    @DisplayName("Dimension extraction: handles truncated or invalid image bytes without throwing")
    void testInvalidBytesHandling() {
        byte[] corrupted = new byte[]{1, 2, 3, 4, 5};
        ImageQualityProber.ImageDimensions dims = prober.extractDimensions(corrupted, "image/jpeg");
        assertNull(dims);
    }
}
