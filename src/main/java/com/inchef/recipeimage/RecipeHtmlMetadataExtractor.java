package com.inchef.recipeimage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecipeHtmlMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(RecipeHtmlMetadataExtractor.class);
    private final ObjectMapper objectMapper;

    public enum CandidateSource {
        JSON_LD,
        OPEN_GRAPH,
        TWITTER_CARD,
        METADATA_LINK,
        HTML_HERO
    }

    public record ImageCandidate(
            String url,
            CandidateSource source,
            Integer declaredWidth,
            Integer declaredHeight,
            String alt
    ) {}

    public RecipeHtmlMetadataExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts an authentic recipe image URL (backward-compatible convenience method).
     */
    public Optional<String> extractImageUrl(String htmlContent, String baseUrl) {
        List<ImageCandidate> candidates = extractImageCandidates(htmlContent, baseUrl, null);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(0).url());
    }

    /**
     * Extracts all legitimate image candidates from JSON-LD, OpenGraph, Twitter, and HTML hero elements.
     */
    public List<ImageCandidate> extractImageCandidates(String htmlContent, String baseUrl) {
        return extractImageCandidates(htmlContent, baseUrl, null);
    }

    /**
     * Extracts and deduplicates all legitimate image candidates from HTML content.
     *
     * @param htmlContent Raw HTML body
     * @param baseUrl     Webpage source URL for resolving relative paths
     * @param recipeTitle Optional recipe title for context-aware hero selection
     * @return Ordered list of unique, valid image candidates
     */
    public List<ImageCandidate> extractImageCandidates(String htmlContent, String baseUrl, String recipeTitle) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return Collections.emptyList();
        }

        List<ImageCandidate> candidates = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {
            Document doc = Jsoup.parse(htmlContent, baseUrl != null ? baseUrl : "");

            // 1. JSON-LD Recipe structured data (support strings, arrays of strings, objects, and arrays of objects)
            extractJsonLdCandidates(doc, baseUrl, candidates, seenUrls);

            // 2. Open Graph Metadata (og:image / og:image:url)
            extractOpenGraphCandidates(doc, baseUrl, candidates, seenUrls);

            // 3. Twitter Card Metadata (twitter:image / twitter:image:src)
            extractTwitterCardCandidates(doc, baseUrl, candidates, seenUrls);

            // 4. Link rel="image_src"
            extractLinkRelCandidates(doc, baseUrl, candidates, seenUrls);

            // 5. HTML Hero/Body Dish Photos (Next.js image proxy decoding, srcset, and article hero images)
            extractHtmlHeroCandidates(doc, baseUrl, recipeTitle, candidates, seenUrls);

        } catch (Exception e) {
            log.warn("Error parsing HTML metadata candidates from {}: {}", baseUrl, e.getMessage());
        }

        return candidates;
    }

    /**
     * Extracts image candidates from JSON-LD <script type="application/ld+json">.
     */
    private void extractJsonLdCandidates(Document doc, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            String json = script.data();
            if (json == null || json.isBlank()) continue;

            try {
                JsonNode root = objectMapper.readTree(json);
                collectJsonLdImages(root, baseUrl, candidates, seenUrls);
            } catch (Exception ignored) {
                // Ignore malformed JSON-LD scripts and continue to next
            }
        }
    }

    private void collectJsonLdImages(JsonNode node, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        if (node == null || node.isNull()) return;

        if (node.isArray()) {
            for (JsonNode item : node) {
                collectJsonLdImages(item, baseUrl, candidates, seenUrls);
            }
            return;
        }

        if (node.isObject()) {
            // Check @graph array if present
            if (node.has("@graph")) {
                collectJsonLdImages(node.get("@graph"), baseUrl, candidates, seenUrls);
            }

            JsonNode typeNode = node.get("@type");
            boolean isRecipeOrArticle = false;
            if (typeNode != null) {
                if (typeNode.isTextual()) {
                    String t = typeNode.asText();
                    if ("Recipe".equalsIgnoreCase(t) || "Article".equalsIgnoreCase(t) || "NewsArticle".equalsIgnoreCase(t)) {
                        isRecipeOrArticle = true;
                    }
                } else if (typeNode.isArray()) {
                    for (JsonNode t : typeNode) {
                        if (t.isTextual() && ("Recipe".equalsIgnoreCase(t.asText()) || "Article".equalsIgnoreCase(t.asText()))) {
                            isRecipeOrArticle = true;
                            break;
                        }
                    }
                }
            }

            if (isRecipeOrArticle) {
                // Check "image" field (can be string, array of strings, object, or array of objects)
                if (node.has("image")) {
                    extractFromJsonLdImageField(node.get("image"), baseUrl, candidates, seenUrls);
                }
                // Check "thumbnailUrl" field
                if (node.has("thumbnailUrl")) {
                    extractFromJsonLdImageField(node.get("thumbnailUrl"), baseUrl, candidates, seenUrls);
                }
            }
        }
    }

    private void extractFromJsonLdImageField(JsonNode imageNode, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        if (imageNode == null || imageNode.isNull()) return;

        if (imageNode.isTextual()) {
            addCandidate(imageNode.asText(), CandidateSource.JSON_LD, null, null, null, baseUrl, candidates, seenUrls);
            return;
        }

        if (imageNode.isArray()) {
            for (JsonNode item : imageNode) {
                extractFromJsonLdImageField(item, baseUrl, candidates, seenUrls);
            }
            return;
        }

        if (imageNode.isObject()) {
            String url = null;
            if (imageNode.has("url") && imageNode.get("url").isTextual()) {
                url = imageNode.get("url").asText();
            } else if (imageNode.has("contentUrl") && imageNode.get("contentUrl").isTextual()) {
                url = imageNode.get("contentUrl").asText();
            }

            Integer declaredWidth = parseIntegerProperty(imageNode, "width");
            Integer declaredHeight = parseIntegerProperty(imageNode, "height");
            String alt = imageNode.has("caption") ? imageNode.get("caption").asText() : null;

            if (url != null) {
                addCandidate(url, CandidateSource.JSON_LD, declaredWidth, declaredHeight, alt, baseUrl, candidates, seenUrls);
            }
        }
    }

    private Integer parseIntegerProperty(JsonNode node, String field) {
        if (node.has(field)) {
            JsonNode f = node.get(field);
            if (f.isInt() || f.isLong()) return f.asInt();
            if (f.isTextual()) {
                try {
                    return Integer.parseInt(f.asText().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Extracts Open Graph candidates from <meta property="og:image"...>.
     */
    private void extractOpenGraphCandidates(Document doc, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Elements metas = doc.select("meta[property=og:image], meta[property=og:image:url], meta[name=og:image]");
        for (Element meta : metas) {
            String content = meta.attr("content");
            if (content == null || content.isBlank()) {
                content = meta.attr("value");
            }
            if (content == null || content.isBlank()) continue;

            Integer width = parseMetaInteger(doc, "meta[property=og:image:width], meta[name=og:image:width]");
            Integer height = parseMetaInteger(doc, "meta[property=og:image:height], meta[name=og:image:height]");
            String alt = meta.attr("alt");

            addCandidate(content, CandidateSource.OPEN_GRAPH, width, height, alt, baseUrl, candidates, seenUrls);
        }
    }

    private Integer parseMetaInteger(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        if (el != null) {
            String val = el.attr("content");
            if (val != null && !val.isBlank()) {
                try {
                    return Integer.parseInt(val.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Extracts Twitter card image candidates.
     */
    private void extractTwitterCardCandidates(Document doc, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Elements metas = doc.select("meta[name=twitter:image], meta[name=twitter:image:src], meta[property=twitter:image]");
        for (Element meta : metas) {
            String content = meta.attr("content");
            if (content == null || content.isBlank()) {
                content = meta.attr("value");
            }
            if (content == null || content.isBlank()) continue;

            addCandidate(content, CandidateSource.TWITTER_CARD, null, null, null, baseUrl, candidates, seenUrls);
        }
    }

    /**
     * Extracts <link rel="image_src"> candidates.
     */
    private void extractLinkRelCandidates(Document doc, String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Elements links = doc.select("link[rel=image_src]");
        for (Element link : links) {
            String href = link.attr("href");
            if (href != null && !href.isBlank()) {
                addCandidate(href, CandidateSource.METADATA_LINK, null, null, null, baseUrl, candidates, seenUrls);
            }
        }
    }

    /**
     * Extracts actual recipe hero/content image candidates from HTML body elements.
     * Unwraps Next.js image proxies (/_next/image?url=...) and parses high-resolution srcset variants.
     */
    private void extractHtmlHeroCandidates(Document doc, String baseUrl, String recipeTitle, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Elements imgs = doc.select("img");
        Pattern logoPattern = Pattern.compile("logo|icon|search|avatar|john_doe|pixel\\.gif", Pattern.CASE_INSENSITIVE);

        for (Element img : imgs) {
            String src = img.attr("src");
            String dataSrc = img.attr("data-src");
            String srcset = img.attr("srcset");
            String alt = img.attr("alt");
            String cls = img.attr("class");

            String candidate = (src != null && !src.isBlank()) ? src : dataSrc;
            if (candidate == null || candidate.isBlank()) continue;

            // Reject clear logos, icons, avatars
            if (logoPattern.matcher(candidate).find() || (alt != null && logoPattern.matcher(alt).find())) {
                continue;
            }

            // Unwrap Next.js image proxy URLs e.g. /_next/image?url=https%3A%2F%2Fstorage...
            String unwrapped = unwrapNextJsImageUrl(candidate);

            // Context check: does image look like a recipe hero dish?
            boolean isHero = false;
            if (cls != null && (cls.contains("rounded") || cls.contains("hero") || cls.contains("featured") || cls.contains("cover"))) {
                isHero = true;
            }
            if (alt != null && !alt.isBlank()) {
                String lowerAlt = alt.toLowerCase();
                if (lowerAlt.contains("recipe") || lowerAlt.contains("dish") || lowerAlt.contains("instructions")) {
                    isHero = true;
                }
                if (recipeTitle != null) {
                    for (String word : recipeTitle.toLowerCase().split("[^a-zA-Z0-9]+")) {
                        if (word.length() >= 4 && lowerAlt.contains(word)) {
                            isHero = true;
                            break;
                        }
                    }
                }
            }

            if (isHero) {
                addCandidate(unwrapped, CandidateSource.HTML_HERO, null, null, alt, baseUrl, candidates, seenUrls);

                // Check srcset for high resolution candidates
                if (srcset != null && !srcset.isBlank()) {
                    String largestSrcset = extractLargestFromSrcset(srcset);
                    if (largestSrcset != null) {
                        String unwrappedSrcset = unwrapNextJsImageUrl(largestSrcset);
                        addCandidate(unwrappedSrcset, CandidateSource.HTML_HERO, null, null, alt, baseUrl, candidates, seenUrls);
                    }
                }
            }
        }

        // Special check: If an existing candidate has "thumbnail_" or "medium_", check if the full un-prefixed filename
        // exists directly in the HTML content (e.g. storage.googleapis.com/.../Dish_Name.jpg)
        extractFullSizeVariantsFromHtml(doc.html(), candidates, seenUrls, baseUrl);
    }

    private void extractFullSizeVariantsFromHtml(String html, List<ImageCandidate> candidates, Set<String> seenUrls, String baseUrl) {
        List<String> currentUrls = new ArrayList<>(seenUrls);
        for (String url : currentUrls) {
            String lower = url.toLowerCase();
            if (lower.contains("thumbnail_") || lower.contains("medium_")) {
                // e.g. Karela_Masala_Recipe_4_ecde9f4393.jpg
                String filename = url.substring(url.lastIndexOf('/') + 1);
                String unPrefixed = filename.replaceFirst("^(?i)(?:thumbnail_|medium_)", "");
                if (unPrefixed.length() >= 8 && html.contains(unPrefixed)) {
                    // Find the full URL in HTML that contains this unPrefixed filename
                    Pattern pat = Pattern.compile("https?://[^\"'\\s<>]+/" + Pattern.quote(unPrefixed));
                    Matcher matcher = pat.matcher(html);
                    if (matcher.find()) {
                        String matchedUrl = matcher.group();
                        addCandidate(matchedUrl, CandidateSource.HTML_HERO, null, null, "Full Resolution Match", baseUrl, candidates, seenUrls);
                    }
                }
            }
        }
    }

    private String unwrapNextJsImageUrl(String url) {
        if (url == null) return null;
        if (url.contains("/_next/image?url=")) {
            try {
                int start = url.indexOf("url=") + 4;
                int end = url.indexOf("&", start);
                String encoded = (end != -1) ? url.substring(start, end) : url.substring(start);
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
        return url;
    }

    private String extractLargestFromSrcset(String srcset) {
        if (srcset == null || srcset.isBlank()) return null;
        String[] entries = srcset.split(",");
        String bestUrl = null;
        int maxDescriptor = 0;

        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 1) {
                String u = parts[0];
                int descValue = 1;
                if (parts.length >= 2) {
                    try {
                        String desc = parts[1].toLowerCase();
                        if (desc.endsWith("w")) {
                            descValue = Integer.parseInt(desc.substring(0, desc.length() - 1));
                        } else if (desc.endsWith("x")) {
                            descValue = (int) (Float.parseFloat(desc.substring(0, desc.length() - 1)) * 1000);
                        }
                    } catch (Exception ignored) {}
                }
                if (descValue >= maxDescriptor) {
                    maxDescriptor = descValue;
                    bestUrl = u;
                }
            }
        }
        return bestUrl;
    }

    private void addCandidate(String candidateUrl, CandidateSource source, Integer width, Integer height, String alt,
                              String baseUrl, List<ImageCandidate> candidates, Set<String> seenUrls) {
        Optional<String> cleaned = cleanAndValidateImageUrl(candidateUrl, baseUrl);
        if (cleaned.isPresent()) {
            String url = cleaned.get();
            if (!seenUrls.contains(url)) {
                seenUrls.add(url);
                candidates.add(new ImageCandidate(url, source, width, height, alt));
            }
        }
    }

    /**
     * Resolves relative URLs, checks schemes, and ensures the candidate is a valid image URL.
     */
    public Optional<String> cleanAndValidateImageUrl(String candidate, String baseUrl) {
        if (candidate == null || candidate.isBlank()) return Optional.empty();

        String trimmed = candidate.trim();

        // Avoid base64 data URIs or javascript schemes
        if (trimmed.startsWith("data:") || trimmed.startsWith("javascript:")) {
            return Optional.empty();
        }

        String absoluteUrl;
        try {
            if (trimmed.startsWith("//")) {
                absoluteUrl = "https:" + trimmed;
            } else if (baseUrl != null && !baseUrl.isBlank()) {
                URI baseUri = URI.create(baseUrl.trim());
                absoluteUrl = baseUri.resolve(trimmed).toString();
            } else {
                absoluteUrl = URI.create(trimmed).toString();
            }
        } catch (Exception e) {
            return Optional.empty();
        }

        if (absoluteUrl == null || absoluteUrl.isBlank()) return Optional.empty();

        // Scheme check
        String lower = absoluteUrl.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return Optional.empty();
        }

        // Never allow the recipe webpage URL itself to be treated as an image URL
        if (baseUrl != null && absoluteUrl.equalsIgnoreCase(baseUrl.trim())) {
            return Optional.empty();
        }

        // Filter out obvious non-image paths like .html, .php, .aspx
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".php") || lower.endsWith(".aspx")) {
            return Optional.empty();
        }

        return Optional.of(absoluteUrl);
    }
}
