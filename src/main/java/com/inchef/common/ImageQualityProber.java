package com.inchef.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ImageQualityProber {

    private static final Logger log = LoggerFactory.getLogger(ImageQualityProber.class);

    private final SsrfValidator ssrfValidator;

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int MAX_PROBE_BYTES = 65536; // 64KB for headers
    private static final int MAX_REDIRECTS = 3;

    private static final Set<String> STOPWORDS = Set.of(
            "recipe", "recipes", "style", "traditional", "authentic", "indian", "with",
            "from", "best", "easy", "home", "made", "dish", "food", "kitchen", "the", "and"
    );

    private static final Pattern LOGO_OR_PLACEHOLDER_PATTERN = Pattern.compile(
            "/(?:logo|logos|icon|icons|avatar|avatars|author|user|users)/|search\\.png|john_doe|pixel\\.gif|blank\\.gif|placeholder",
            Pattern.CASE_INSENSITIVE
    );

    public record ImageDimensions(int width, int height) {
        public int getArea() {
            return width * height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    public record ProbedImage(
            String url,
            String source,
            ImageDimensions dimensions,
            String contentType,
            int qualityScore,
            boolean accessible,
            String reason
    ) {}

    public ImageQualityProber(SsrfValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
    }

    /**
     * Safely probes an image candidate by validating status, content type, and dimensions.
     */
    public ProbedImage probe(String candidateUrl, String source, String recipeTitle) {
        if (candidateUrl == null || candidateUrl.isBlank()) {
            return new ProbedImage(candidateUrl, source, null, null, -100, false, "Empty URL");
        }

        String targetUrl = candidateUrl.trim();

        // 1. SSRF validation
        if (!ssrfValidator.isSafeUrl(targetUrl)) {
            return new ProbedImage(targetUrl, source, null, null, -100, false, "Blocked by SSRF validator");
        }

        // 2. Reject obvious logos, icons, avatars, and placeholders from path
        if (LOGO_OR_PLACEHOLDER_PATTERN.matcher(targetUrl).find()) {
            return new ProbedImage(targetUrl, source, null, null, -50, false, "Rejected: Logo or placeholder path");
        }

        String currentUrl = targetUrl;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpURLConnection conn = null;
            try {
                URI uri = URI.create(currentUrl);
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) InChefQualityProbe/2.0");
                conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

                int status = conn.getResponseCode();

                // Handle Redirects safely
                if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {

                    String location = conn.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        return new ProbedImage(targetUrl, source, null, null, -50, false, "Redirect without Location header");
                    }
                    URI redirectedUri = uri.resolve(location.trim());
                    currentUrl = redirectedUri.toString();

                    if (!ssrfValidator.isSafeUrl(currentUrl)) {
                        return new ProbedImage(targetUrl, source, null, null, -100, false, "Redirect destination blocked by SSRF");
                    }
                    continue;
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    return new ProbedImage(targetUrl, source, null, null, -30, false, "HTTP " + status);
                }

                String contentType = conn.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    return new ProbedImage(targetUrl, source, null, contentType, -40, false, "Non-image Content-Type: " + contentType);
                }

                // Reject SVG files as dish photos (usually icons/logos)
                if (contentType.toLowerCase().contains("svg")) {
                    return new ProbedImage(targetUrl, source, null, contentType, -20, false, "SVG images rejected as dish photos");
                }

                // Stream up to 64KB bounded content for dimension analysis
                byte[] headerBytes;
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalRead = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (totalRead >= MAX_PROBE_BYTES) {
                            break;
                        }
                    }
                    headerBytes = out.toByteArray();
                }

                ImageDimensions dimensions = extractDimensions(headerBytes, contentType);

                // Reject tiny thumbnails, icons, and tracking pixels
                if (dimensions != null && (dimensions.width() < 80 || dimensions.height() < 80)) {
                    return new ProbedImage(targetUrl, source, dimensions, contentType, -20, false,
                            "Image dimensions too small (< 80px): " + dimensions);
                }

                int score = calculateQualityScore(targetUrl, source, dimensions, recipeTitle);
                String reason = dimensions != null
                        ? String.format("Verified %dx%d (Score: %d)", dimensions.width(), dimensions.height(), score)
                        : String.format("Verified format %s with unknown dimensions (Score: %d)", contentType, score);

                return new ProbedImage(targetUrl, source, dimensions, contentType, score, true, reason);

            } catch (Exception e) {
                log.debug("Probing failed for candidate {}: {}", targetUrl, e.getMessage());
                return new ProbedImage(targetUrl, source, null, null, -30, false, "Probe exception: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        return new ProbedImage(targetUrl, source, null, null, -30, false, "Exceeded maximum redirects");
    }

    /**
     * Extracts width and height safely from the initial image header chunk.
     */
    public ImageDimensions extractDimensions(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) return null;

        // Try standard ImageIO readers (JPEG, PNG, GIF, BMP, TIFF)
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (iis != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true, true);
                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        if (width > 0 && height > 0) {
                            return new ImageDimensions(width, height);
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback to custom WebP header parser if applicable
        }

        // WebP format dimension header parser
        return parseWebpDimensions(imageBytes);
    }

    /**
     * Lightweight binary parser for WebP headers (VP8, VP8L, VP8X).
     */
    private ImageDimensions parseWebpDimensions(byte[] bytes) {
        if (bytes == null || bytes.length < 30) return null;

        // Check "RIFF" .... "WEBP"
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
            bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {

            // VP8 (lossy)
            if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == ' ') {
                if (bytes.length >= 30) {
                    int w = ((bytes[26] & 0xFF) | ((bytes[27] & 0x3F) << 8));
                    int h = ((bytes[28] & 0xFF) | ((bytes[29] & 0x3F) << 8));
                    if (w > 0 && h > 0) return new ImageDimensions(w, h);
                }
            }
            // VP8L (lossless)
            else if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'L') {
                if (bytes.length >= 25 && bytes[20] == 0x2f) {
                    int b1 = bytes[21] & 0xFF;
                    int b2 = bytes[22] & 0xFF;
                    int b3 = bytes[23] & 0xFF;
                    int b4 = bytes[24] & 0xFF;
                    int w = 1 + (((b2 & 0x3F) << 8) | b1);
                    int h = 1 + (((b4 & 0xF) << 10) | (b3 << 2) | ((b2 & 0xC0) >> 6));
                    if (w > 0 && h > 0) return new ImageDimensions(w, h);
                }
            }
            // VP8X (extended)
            else if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'X') {
                if (bytes.length >= 30) {
                    int w = 1 + ((bytes[24] & 0xFF) | ((bytes[25] & 0xFF) << 8) | ((bytes[26] & 0xFF) << 16));
                    int h = 1 + ((bytes[27] & 0xFF) | ((bytes[28] & 0xFF) << 8) | ((bytes[29] & 0xFF) << 16));
                    if (w > 0 && h > 0) return new ImageDimensions(w, h);
                }
            }
        }
        return null;
    }

    /**
     * Calculates candidate quality score based on actual dimensions, source, keywords, and thumbnail penalties.
     */
    private int calculateQualityScore(String url, String source, ImageDimensions dimensions, String recipeTitle) {
        int score = 0;

        // 1. Resolution score based on area
        if (dimensions != null) {
            int area = dimensions.getArea();
            if (area >= 1_000_000) {
                score += 100; // e.g. 1600x1200, 1920x1080
            } else if (area >= 400_000) {
                score += 80;  // e.g. 800x600, 750x600
            } else if (area >= 150_000) {
                score += 65;  // e.g. 500x400
            } else if (area >= 60_000) {
                score += 45;  // e.g. 300x250
            } else {
                score += 15;  // tiny thumbnail (< 250x250)
            }
        } else {
            // Unknown dimensions, valid image
            score += 30;
        }

        // 2. Source credibility bonus
        if (source != null) {
            String s = source.toUpperCase();
            if (s.contains("JSON_LD")) {
                score += 15;
            } else if (s.contains("OPEN_GRAPH") || s.contains("OG")) {
                score += 15;
            } else if (s.contains("TWITTER")) {
                score += 10;
            } else if (s.contains("HTML_HERO") || s.contains("HERO")) {
                score += 12;
            } else {
                score += 5;
            }
        }

        // 3. Keyword matching bonus
        if (recipeTitle != null && !recipeTitle.isBlank()) {
            String lowerUrl = url.toLowerCase();
            String[] tokens = recipeTitle.toLowerCase().split("[^a-zA-Z0-9]+");
            int matches = 0;
            for (String token : tokens) {
                if (token.length() >= 4 && !STOPWORDS.contains(token) && lowerUrl.contains(token)) {
                    matches++;
                }
            }
            if (matches > 0) {
                score += Math.min(20, matches * 10);
            }
        }

        // 4. Thumbnail penalties
        String lower = url.toLowerCase();
        if (lower.contains("thumbnail") || lower.contains("thumb_") || lower.contains("/thumb/")) {
            score -= 35;
        } else if (lower.contains("medium_")) {
            score -= 5;
        }

        return score;
    }
}
