package com.inchef.recipeimage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RecipeImageAuditReport {

    private long totalRecipes;
    private long existingImagesReused;
    private long imagesExtractedFromMetadata;
    private long imagesFromSearch;
    private long placeholdersCount;
    private long failedCount;
    private long pendingCount;
    private long uniqueNonemptyImageCount;
    private long repeatedImageCount;
    private List<Map<String, Object>> repeatedImageDetails;
    private LocalDateTime generatedAt;

    public RecipeImageAuditReport() {
        this.generatedAt = LocalDateTime.now();
    }

    public long getTotalRecipes() { return totalRecipes; }
    public void setTotalRecipes(long totalRecipes) { this.totalRecipes = totalRecipes; }

    public long getExistingImagesReused() { return existingImagesReused; }
    public void setExistingImagesReused(long existingImagesReused) { this.existingImagesReused = existingImagesReused; }

    public long getImagesExtractedFromMetadata() { return imagesExtractedFromMetadata; }
    public void setImagesExtractedFromMetadata(long imagesExtractedFromMetadata) { this.imagesExtractedFromMetadata = imagesExtractedFromMetadata; }

    public long getImagesFromSearch() { return imagesFromSearch; }
    public void setImagesFromSearch(long imagesFromSearch) { this.imagesFromSearch = imagesFromSearch; }

    public long getPlaceholdersCount() { return placeholdersCount; }
    public void setPlaceholdersCount(long placeholdersCount) { this.placeholdersCount = placeholdersCount; }

    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getUniqueNonemptyImageCount() { return uniqueNonemptyImageCount; }
    public void setUniqueNonemptyImageCount(long uniqueNonemptyImageCount) { this.uniqueNonemptyImageCount = uniqueNonemptyImageCount; }

    public long getRepeatedImageCount() { return repeatedImageCount; }
    public void setRepeatedImageCount(long repeatedImageCount) { this.repeatedImageCount = repeatedImageCount; }

    public List<Map<String, Object>> getRepeatedImageDetails() { return repeatedImageDetails; }
    public void setRepeatedImageDetails(List<Map<String, Object>> repeatedImageDetails) { this.repeatedImageDetails = repeatedImageDetails; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
