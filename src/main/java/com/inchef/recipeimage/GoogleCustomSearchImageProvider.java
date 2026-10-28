package com.inchef.recipeimage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.inchef.common.SsrfValidator;

@Service
public class GoogleCustomSearchImageProvider implements ImageSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleCustomSearchImageProvider.class);

    private final String apiKey;
    private final String cx;
    private final SsrfValidator ssrfValidator;
    private final ObjectMapper objectMapper;

    public GoogleCustomSearchImageProvider(
            @Value("${GOOGLE_CUSTOM_SEARCH_API_KEY:${inchef.image-search.google.api-key:}}") String apiKey,
            @Value("${GOOGLE_CUSTOM_SEARCH_CX:${inchef.image-search.google.cx:}}") String cx,
            SsrfValidator ssrfValidator,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.cx = cx != null ? cx.trim() : "";
        this.ssrfValidator = ssrfValidator;
        this.objectMapper = objectMapper;

        if (isAvailable()) {
            log.info("Google Custom Search Image Provider is configured and active.");
        } else {
            log.info("Google Custom Search Image Provider credentials not supplied; Strategy C will be gracefully bypassed.");
        }
    }

    @Override
    public String getProviderName() {
        return "GoogleCustomSearch";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank() && !cx.isBlank();
    }

    @Override
    public Optional<String> searchImage(String recipeTitle, String cuisine, List<String> mainIngredients) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        if (recipeTitle == null || recipeTitle.isBlank()) {
            return Optional.empty();
        }

        try {
            // Construct specific query: exact recipe title + cuisine / authentic
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("\"").append(recipeTitle.trim()).append("\"");
            if (cuisine != null && !cuisine.isBlank()) {
                queryBuilder.append(" ").append(cuisine.trim());
            } else {
                queryBuilder.append(" Indian recipe");
            }

            if (mainIngredients != null && !mainIngredients.isEmpty()) {
                queryBuilder.append(" ").append(mainIngredients.get(0));
            }

            String encodedQuery = URLEncoder.encode(queryBuilder.toString(), StandardCharsets.UTF_8);
            String urlStr = String.format(
                    "https://customsearch.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&num=3&safe=active",
                    apiKey, cx, encodedQuery
            );

            ssrfValidator.validateUrl(urlStr);

            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "InChef-Image-Engine/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Google Custom Search returned HTTP status {} for query: {}", status, queryBuilder);
                return Optional.empty();
            }

            try (InputStream in = conn.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                JsonNode items = root.get("items");
                if (items != null && items.isArray() && items.size() > 0) {
                    for (JsonNode item : items) {
                        String link = item.has("link") ? item.get("link").asText() : null;
                        String itemTitle = item.has("title") ? item.get("title").asText().toLowerCase() : "";

                        if (link != null && ssrfValidator.isSafeUrl(link)) {
                            // Filter out completely unrelated results: verify title relevance
                            String cleanTitle = recipeTitle.toLowerCase().replaceAll("[^a-z0-9]", " ");
                            String[] titleWords = cleanTitle.split("\\s+");
                            int matches = 0;
                            for (String w : titleWords) {
                                if (w.length() > 3 && itemTitle.contains(w)) {
                                    matches++;
                                }
                            }

                            // If at least one distinctive keyword matches, accept candidate
                            if (matches > 0 || itemTitle.contains("recipe") || itemTitle.contains("curry") || itemTitle.contains("dish")) {
                                log.info("Strategy C (Google Search) resolved authentic image for '{}': {}", recipeTitle, link);
                                return Optional.of(link);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Strategy C image search failed for '{}': {}", recipeTitle, e.getMessage());
        }

        return Optional.empty();
    }
}
