package com.inchef.recipeimage;

import java.time.LocalDateTime;

public class RecipeImageBatchStatus {

    private int totalProcessed;
    private int reusedExistingCount;
    private int metadataResolvedCount;
    private int searchResolvedCount;
    private int placeholderAssignedCount;
    private int failedCount;
    private boolean dryRun;
    private int batchSize;
    private int limit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMillis;
    private String statusMessage;

    public RecipeImageBatchStatus() {
        this.startTime = LocalDateTime.now();
    }

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getReusedExistingCount() { return reusedExistingCount; }
    public void setReusedExistingCount(int reusedExistingCount) { this.reusedExistingCount = reusedExistingCount; }

    public int getMetadataResolvedCount() { return metadataResolvedCount; }
    public void setMetadataResolvedCount(int metadataResolvedCount) { this.metadataResolvedCount = metadataResolvedCount; }

    public int getSearchResolvedCount() { return searchResolvedCount; }
    public void setSearchResolvedCount(int searchResolvedCount) { this.searchResolvedCount = searchResolvedCount; }

    public int getPlaceholderAssignedCount() { return placeholderAssignedCount; }
    public void setPlaceholderAssignedCount(int placeholderAssignedCount) { this.placeholderAssignedCount = placeholderAssignedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}
