package com.inchef.recipeimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.inchef.common.ImageQualityProber;
import com.inchef.common.SsrfValidator;
import com.inchef.exception.ResourceNotFoundException;
import com.inchef.recipe.Recipe;
import com.inchef.recipe.RecipeRepository;

@Service
public class RecipeImageResolutionService {

    private static final Logger log = LoggerFactory.getLogger(RecipeImageResolutionService.class);

    public static final String NEUTRAL_PLACEHOLDER = "/images/neutral_recipe_placeholder.svg";
    private static final int MAX_RESPONSE_BYTES = 512 * 1024; // 512 KB stream limit
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_REDIRECTS = 3;

    private final RecipeRepository recipeRepository;
    private final SsrfValidator ssrfValidator;
    private final RecipeHtmlMetadataExtractor metadataExtractor;
    private final ImageSearchProvider imageSearchProvider;
    private final ImageQualityProber imageQualityProber;

    // In-memory cache for resolved URLs in the current runtime
    private final Map<String, String> resolvedUrlCache = new ConcurrentHashMap<>();
    private volatile RecipeImageBatchStatus currentBatchStatus = null;

    @Autowired
    public RecipeImageResolutionService(
            RecipeRepository recipeRepository,
            SsrfValidator ssrfValidator,
            RecipeHtmlMetadataExtractor metadataExtractor,
            ImageSearchProvider imageSearchProvider,
            ImageQualityProber imageQualityProber) {
        this.recipeRepository = recipeRepository;
        this.ssrfValidator = ssrfValidator;
        this.metadataExtractor = metadataExtractor;
        this.imageSearchProvider = imageSearchProvider;
        this.imageQualityProber = imageQualityProber != null ? imageQualityProber : new ImageQualityProber(ssrfValidator);
    }

    public RecipeImageResolutionService(
            RecipeRepository recipeRepository,
            SsrfValidator ssrfValidator,
            RecipeHtmlMetadataExtractor metadataExtractor,
            ImageSearchProvider imageSearchProvider) {
        this(recipeRepository, ssrfValidator, metadataExtractor, imageSearchProvider, new ImageQualityProber(ssrfValidator));
    }

    /**
     * Resolves an image for a specific recipe using Priorities A -> B -> C -> D.
     *
     * @param recipeId Recipe ID
     * @param force    If true, re-attempts resolution even if previously resolved
     * @return Resulting RecipeImageResponse
     */
    @Transactional
    public RecipeImageResponse resolveRecipeImage(Long recipeId, boolean force) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        resolveSingleRecipe(recipe, force, false);
        Recipe saved = recipeRepository.save(recipe);
        return mapToImageResponse(saved);
    }

    /**
     * Retrieves the current image metadata for a recipe.
     */
    @Transactional(readOnly = true)
    public RecipeImageResponse getRecipeImageMetadata(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        return mapToImageResponse(recipe);
    }

    /**
     * Internal resolution logic applying Strategies A -> B -> C -> D.
     */
    public boolean resolveSingleRecipe(Recipe recipe, boolean force, boolean dryRun) {
        String currentImg = recipe.getImageUrl();
        LocalDateTime now = LocalDateTime.now();
        String sourceUrl = recipe.getSourceUrl();
        boolean hasExisting = isValidExistingImage(currentImg, sourceUrl);

        ImageQualityProber.ProbedImage currentProbed = null;
        if (hasExisting) {
            currentProbed = imageQualityProber.probe(currentImg, "EXISTING", recipe.getTitle());
        }

        // If not force, and we have an existing high-resolution image (score >= 80 and not a thumbnail), preserve it:
        if (!force && hasExisting && currentProbed != null && currentProbed.accessible() && currentProbed.qualityScore() >= 80) {
            if (!dryRun) {
                if (recipe.getImageStatus() == null || recipe.getImageStatus() == ImageStatus.PENDING) {
                    recipe.setImageStatus(ImageStatus.RESOLVED);
                }
                if (recipe.getImageSourceType() == null) {
                    recipe.setImageSourceType(ImageSourceType.WEBPAGE_METADATA);
                }
                if (recipe.getImageSourceUrl() == null) {
                    recipe.setImageSourceUrl(recipe.getSourceUrl());
                }
                recipe.setImageLastCheckedAt(now);
            }
            log.debug("Strategy A: Recipe {} already has verified high-quality image: {}", recipe.getId(), currentImg);
            return true;
        }

        // -------------------------------------------------------------
        // Strategy B: Extract and rank candidates from original recipe webpage
        // -------------------------------------------------------------
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            Optional<ImageQualityProber.ProbedImage> bestCandidateOpt = fetchAndSelectBestImage(sourceUrl, recipe.getTitle());
            if (bestCandidateOpt.isPresent()) {
                ImageQualityProber.ProbedImage best = bestCandidateOpt.get();

                // If recipe already has a working image, only replace if best candidate is strictly better quality
                if (hasExisting && currentProbed != null && currentProbed.accessible() && currentProbed.qualityScore() >= best.qualityScore()) {
                    log.info("Keeping existing image for recipe {}: current score {} >= candidate score {}",
                            recipe.getId(), currentProbed.qualityScore(), best.qualityScore());
                    if (!dryRun) {
                        recipe.setImageStatus(ImageStatus.RESOLVED);
                        recipe.setImageLastCheckedAt(now);
                    }
                    return true;
                }

                String imgUrl = best.url();
                resolvedUrlCache.put(sourceUrl, imgUrl);
                if (!dryRun) {
                    recipe.setImageUrl(imgUrl);
                    recipe.setImageSourceUrl(sourceUrl);
                    recipe.setImageSourceType(ImageSourceType.WEBPAGE_METADATA);
                    recipe.setImageStatus(ImageStatus.RESOLVED);
                    recipe.setImageLastCheckedAt(now);
                }
                log.info("Strategy B: Resolved highest-quality image for recipe {}: {} (score: {}, dim: {})",
                        recipe.getId(), imgUrl, best.qualityScore(), best.dimensions());
                return true;
            }
        }

        // If existing image is valid and no better candidate was found from source, keep existing image
        if (hasExisting && currentProbed != null && currentProbed.accessible()) {
            if (!dryRun) {
                recipe.setImageStatus(ImageStatus.RESOLVED);
                recipe.setImageLastCheckedAt(now);
            }
            log.info("Keeping existing image for recipe {}: no better candidate verified from source", recipe.getId());
            return true;
        }

        // -------------------------------------------------------------
        // Strategy C: Authorized image-search fallback
        // -------------------------------------------------------------
        if (imageSearchProvider.isAvailable()) {
            List<String> ingredients = (recipe.getRecipeIngredients() != null)
                    ? recipe.getRecipeIngredients().stream()
                        .filter(ri -> ri.getIngredient() != null)
                        .map(ri -> ri.getIngredient().getName())
                        .limit(3)
                        .collect(Collectors.toList())
                    : Collections.emptyList();

            Optional<String> searchResult = imageSearchProvider.searchImage(
                    recipe.getTitle(), recipe.getCuisine(), ingredients);

            if (searchResult.isPresent()) {
                String imgUrl = searchResult.get();
                if (!dryRun) {
                    recipe.setImageUrl(imgUrl);
                    recipe.setImageSourceUrl(imageSearchProvider.getProviderName());
                    recipe.setImageSourceType(ImageSourceType.IMAGE_SEARCH);
                    recipe.setImageStatus(ImageStatus.RESOLVED);
                    recipe.setImageLastCheckedAt(now);
                }
                log.info("Strategy C: Resolved image from search provider for recipe {}: {}", recipe.getId(), imgUrl);
                return true;
            }
        }

        // -------------------------------------------------------------
        // Strategy D: Neutral Placeholder
        // -------------------------------------------------------------
        if (!dryRun) {
            recipe.setImageUrl(NEUTRAL_PLACEHOLDER);
            recipe.setImageSourceType(ImageSourceType.PLACEHOLDER);
            recipe.setImageStatus(ImageStatus.PLACEHOLDER);
            recipe.setImageLastCheckedAt(now);
        }
        log.debug("Strategy D: Assigned neutral placeholder to recipe {}", recipe.getId());
        return false;
    }

    /**
     * Executes batch resolution across pending/failed recipes in bounded chunks.
     *
     * @param limit     Max number of recipes to process
     * @param batchSize Batch chunk size (commits per chunk)
     * @param dryRun    If true, simulates without saving to DB
     * @param force     If true, processes even already resolved recipes
     * @return Batch execution summary status
     */
    public RecipeImageBatchStatus executeBatchResolution(int limit, int batchSize, boolean dryRun, boolean force) {
        RecipeImageBatchStatus status = new RecipeImageBatchStatus();
        status.setLimit(limit);
        status.setBatchSize(batchSize);
        status.setDryRun(dryRun);
        status.setStatusMessage("Resolution in progress");
        this.currentBatchStatus = status;

        long startTime = System.currentTimeMillis();
        int processedTotal = 0;
        int pageIndex = 0;

        try {
            while (processedTotal < limit) {
                int pageSize = Math.min(batchSize, limit - processedTotal);
                Page<Recipe> page;

                if (force) {
                    page = recipeRepository.findAll(PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id")));
                    pageIndex++;
                } else if (dryRun) {
                    List<ImageStatus> eligible = List.of(ImageStatus.PENDING, ImageStatus.FAILED);
                    page = recipeRepository.findByImageStatusIn(eligible, PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id")));
                    pageIndex++;
                } else {
                    List<ImageStatus> eligible = List.of(ImageStatus.PENDING, ImageStatus.FAILED);
                    page = recipeRepository.findByImageStatusIn(eligible, PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "id")));
                }

                List<Recipe> recipes = page.getContent();
                if (recipes.isEmpty()) {
                    break;
                }

                List<Recipe> batchToSave = new ArrayList<>();
                for (Recipe r : recipes) {
                    if (processedTotal >= limit) break;

                    boolean hadValid = isValidExistingImage(r.getImageUrl(), r.getSourceUrl());
                    boolean resolved = resolveSingleRecipe(r, force, dryRun);
                    processedTotal++;

                    if (hadValid && !force) {
                        status.setReusedExistingCount(status.getReusedExistingCount() + 1);
                    } else if (resolved) {
                        if (r.getImageSourceType() == ImageSourceType.IMAGE_SEARCH) {
                            status.setSearchResolvedCount(status.getSearchResolvedCount() + 1);
                        } else {
                            status.setMetadataResolvedCount(status.getMetadataResolvedCount() + 1);
                        }
                    } else {
                        status.setPlaceholderAssignedCount(status.getPlaceholderAssignedCount() + 1);
                    }

                    batchToSave.add(r);

                    // Polite crawling delay between external requests
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (!dryRun && !batchToSave.isEmpty()) {
                    recipeRepository.saveAll(batchToSave);
                }
            }

            status.setTotalProcessed(processedTotal);
            status.setEndTime(LocalDateTime.now());
            status.setDurationMillis(System.currentTimeMillis() - startTime);
            status.setStatusMessage("Completed successfully. Processed " + processedTotal + " recipes.");

        } catch (Exception e) {
            log.error("Batch image resolution error: {}", e.getMessage(), e);
            status.setStatusMessage("Failed with error: " + e.getMessage());
            status.setEndTime(LocalDateTime.now());
            status.setDurationMillis(System.currentTimeMillis() - startTime);
        }

        return status;
    }

    /**
     * Returns current running or last executed batch status.
     */
    public RecipeImageBatchStatus getCurrentBatchStatus() {
        if (currentBatchStatus == null) {
            RecipeImageBatchStatus status = new RecipeImageBatchStatus();
            status.setStatusMessage("No batch resolution has been executed yet in this session.");
            return status;
        }
        return currentBatchStatus;
    }

    /**
     * Generates a comprehensive quantitative image audit report.
     */
    @Transactional(readOnly = true)
    public RecipeImageAuditReport generateAuditReport() {
        RecipeImageAuditReport report = new RecipeImageAuditReport();

        long total = recipeRepository.count();
        long resolved = recipeRepository.countByImageStatus(ImageStatus.RESOLVED);
        long pending = recipeRepository.countByImageStatus(ImageStatus.PENDING);
        long failed = recipeRepository.countByImageStatus(ImageStatus.FAILED);
        long placeholder = recipeRepository.countByImageStatus(ImageStatus.PLACEHOLDER);
        long uniqueImages = recipeRepository.countDistinctValidImages();

        List<Object[]> repeatedRows = recipeRepository.findRepeatedImages();
        List<Map<String, Object>> repeatedDetails = new ArrayList<>();
        long repeatedImageCount = 0;

        for (Object[] row : repeatedRows) {
            String url = (String) row[0];
            Long count = (Long) row[1];
            repeatedImageCount++;
            repeatedDetails.add(Map.of("imageUrl", url, "recipeCount", count));
        }

        report.setTotalRecipes(total);
        report.setExistingImagesReused(resolved);
        report.setImagesExtractedFromMetadata(resolved);
        report.setImagesFromSearch(0);
        report.setPlaceholdersCount(placeholder);
        report.setPendingCount(pending);
        report.setFailedCount(failed);
        report.setUniqueNonemptyImageCount(uniqueImages);
        report.setRepeatedImageCount(repeatedImageCount);
        report.setRepeatedImageDetails(repeatedDetails);

        return report;
    }

    /**
     * Safely fetches HTML from source URL and extracts/ranks image candidates by quality.
     */
    public Optional<String> fetchAndExtractMetadataImage(String sourceUrl) {
        return fetchAndExtractMetadataImage(sourceUrl, null);
    }

    public Optional<String> fetchAndExtractMetadataImage(String sourceUrl, String recipeTitle) {
        return fetchAndSelectBestImage(sourceUrl, recipeTitle).map(ImageQualityProber.ProbedImage::url);
    }

    public Optional<ImageQualityProber.ProbedImage> fetchAndSelectBestImage(String sourceUrl, String recipeTitle) {
        try {
            String html = fetchHtmlContent(sourceUrl);
            if (html == null || html.isBlank()) {
                return Optional.empty();
            }
            return selectBestCandidateFromHtml(html, sourceUrl, recipeTitle);
        } catch (Exception e) {
            log.debug("Failed to fetch or parse {}: {}", sourceUrl, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Safely fetches bounded HTML content with SSRF checks, timeouts, and redirect handling.
     */
    protected String fetchHtmlContent(String sourceUrl) throws IOException {
        String currentUrl = sourceUrl;

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            if (!ssrfValidator.isSafeUrl(currentUrl)) {
                log.warn("Blocked unsafe or restricted URL: {}", currentUrl);
                return null;
            }

            HttpURLConnection conn = null;
            try {
                URI uri = URI.create(currentUrl);
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(false); // Manually handle redirects to validate destination IP
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 InChefRecipeBot/2.0");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");

                int status = conn.getResponseCode();

                // Handle Redirects safely
                if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {

                    String location = conn.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        return null;
                    }
                    URI redirectedUri = uri.resolve(location.trim());
                    currentUrl = redirectedUri.toString();
                    continue;
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    log.debug("HTTP status {} received from {}", status, currentUrl);
                    return null;
                }

                // Stream bounded content up to MAX_RESPONSE_BYTES
                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalRead = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (totalRead >= MAX_RESPONSE_BYTES) {
                            break;
                        }
                    }
                    return out.toString(StandardCharsets.UTF_8);
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        return null;
    }

    /**
     * Extracts candidates from HTML, probes each with safety checks, and selects the highest ranked candidate.
     */
    public Optional<ImageQualityProber.ProbedImage> selectBestCandidateFromHtml(
            String html, String pageUrl, String recipeTitle) {

        List<RecipeHtmlMetadataExtractor.ImageCandidate> candidates =
                metadataExtractor.extractImageCandidates(html, pageUrl, recipeTitle);

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        List<ImageQualityProber.ProbedImage> probedCandidates = new ArrayList<>();
        for (RecipeHtmlMetadataExtractor.ImageCandidate candidate : candidates) {
            ImageQualityProber.ProbedImage probed = imageQualityProber.probe(
                    candidate.url(), candidate.source().name(), recipeTitle);
            if (probed != null && probed.accessible() && probed.qualityScore() > 0) {
                probedCandidates.add(probed);
            }
        }

        if (probedCandidates.isEmpty()) {
            // Fallback to highest declared dimension or first SSRF-safe candidate if all probes timed out
            for (RecipeHtmlMetadataExtractor.ImageCandidate c : candidates) {
                if (ssrfValidator.isSafeUrl(c.url())) {
                    return Optional.of(new ImageQualityProber.ProbedImage(
                            c.url(), c.source().name(), null, "image/jpeg", 25, true, "Fallback candidate without live probe"));
                }
            }
            return Optional.empty();
        }

        // Sort by quality score descending, then by area descending
        probedCandidates.sort((a, b) -> {
            int scoreCmp = Integer.compare(b.qualityScore(), a.qualityScore());
            if (scoreCmp != 0) return scoreCmp;
            int aArea = a.dimensions() != null ? a.dimensions().getArea() : 0;
            int bArea = b.dimensions() != null ? b.dimensions().getArea() : 0;
            return Integer.compare(bArea, aArea);
        });

        return Optional.of(probedCandidates.get(0));
    }

    public static class ProxiedImage {
        private final byte[] data;
        private final String contentType;

        public ProxiedImage(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        public byte[] getData() { return data; }
        public String getContentType() { return contentType; }
    }

    private final Map<String, ProxiedImage> imageBytesCache = new ConcurrentHashMap<>();

    /**
     * Proxies a verified remote image with SSRF checks, content-type validation, and caching.
     * Prevents third-party hotlink 403 Forbidden errors in user browsers.
     */
    public Optional<ProxiedImage> proxyImage(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank() || !ssrfValidator.isSafeUrl(targetUrl)) {
            return Optional.empty();
        }

        if (imageBytesCache.containsKey(targetUrl)) {
            return Optional.of(imageBytesCache.get(targetUrl));
        }

        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(targetUrl);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            // CRITICAL: Do NOT send Referer header (bypasses third-party CDN hotlink blocks)
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 InChefProxy/2.0");
            conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                log.debug("Proxy image request returned HTTP status {} for {}", status, targetUrl);
                return Optional.empty();
            }

            String contentType = conn.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                log.debug("Proxy rejected non-image content-type for {}: {}", targetUrl, contentType);
                return Optional.empty();
            }

            byte[] bytes;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                int total = 0;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    total += read;
                    if (total > 2 * 1024 * 1024) { // 2MB limit
                        break;
                    }
                }
                bytes = out.toByteArray();
            }

            ProxiedImage img = new ProxiedImage(bytes, contentType);
            if (imageBytesCache.size() < 1000) {
                imageBytesCache.put(targetUrl, img);
            }
            return Optional.of(img);

        } catch (Exception e) {
            log.debug("Failed to proxy image from {}: {}", targetUrl, e.getMessage());
            return Optional.empty();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean isValidExistingImage(String imageUrl, String sourceUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;
        String trimmed = imageUrl.trim();
        if (trimmed.equals(NEUTRAL_PLACEHOLDER)) return false;
        if (trimmed.startsWith("/images/veg_pulao") || trimmed.startsWith("/images/rustic_countertop")) return false;
        if (sourceUrl != null && trimmed.equalsIgnoreCase(sourceUrl.trim())) return false;
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && ssrfValidator.isSafeUrl(trimmed);
    }

    private RecipeImageResponse mapToImageResponse(Recipe r) {
        return new RecipeImageResponse(
                r.getId(),
                r.getTitle(),
                r.getSourceRecipeId(),
                r.getSourceUrl(),
                r.getImageUrl(),
                r.getImageSourceUrl(),
                r.getImageSourceType(),
                r.getImageStatus(),
                r.getImageLastCheckedAt(),
                r.getImageContentHash()
        );
    }
}
