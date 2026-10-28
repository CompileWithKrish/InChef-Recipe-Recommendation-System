package com.inchef.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfValidator();
    }

    @Test
    @DisplayName("Reject null, empty, and malformed URLs")
    void testInvalidUrls() {
        assertFalse(validator.isSafeUrl(null));
        assertFalse(validator.isSafeUrl(""));
        assertFalse(validator.isSafeUrl("   "));
        assertFalse(validator.isSafeUrl("not a url"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/image.jpg",
            "file:///etc/passwd",
            "gopher://example.com:70",
            "javascript:alert(1)",
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    })
    @DisplayName("Reject non-HTTP/HTTPS schemes")
    void testUnsupportedSchemes(String url) {
        assertFalse(validator.isSafeUrl(url));
        assertThrows(SecurityException.class, () -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/image.jpg",
            "http://127.0.0.1/dish.png",
            "http://127.0.0.2:8000/test.webp",
            "http://metadata.google.internal/computeMetadata/v1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://instance-data/latest/meta-data/"
    })
    @DisplayName("Reject loopback, localhost, and cloud metadata targets")
    void testLocalhostAndCloudMetadata(String url) {
        assertFalse(validator.isSafeUrl(url));
    }

    @Test
    @DisplayName("Test private IP detection directly via InetAddress")
    void testRestrictedIpAddresses() throws Exception {
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("127.0.0.1")));
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("10.0.0.1")));
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("172.16.0.1")));
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("192.168.1.1")));
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("169.254.169.254")));
        assertTrue(validator.isRestrictedAddress(InetAddress.getByName("0.0.0.0")));

        // Public IPs should not be restricted
        assertFalse(validator.isRestrictedAddress(InetAddress.getByName("8.8.8.8")));
        assertFalse(validator.isRestrictedAddress(InetAddress.getByName("1.1.1.1")));
    }

    @Test
    @DisplayName("Accept valid public HTTPS URLs")
    void testValidPublicUrls() {
        assertTrue(validator.isSafeUrl("https://images.archanaskitchen.com/recipe.jpg"));
        assertTrue(validator.isSafeUrl("http://example.com/photos/food.webp"));
    }
}
