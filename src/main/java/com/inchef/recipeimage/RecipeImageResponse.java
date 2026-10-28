package com.inchef.recipeimage;

import java.time.LocalDateTime;

public class RecipeImageResponse {

    private Long recipeId;
    private String title;
    private String sourceRecipeId;
    private String sourceUrl;
    private String imageUrl;
    private String imageSourceUrl;
    private ImageSourceType imageSourceType;
    private ImageStatus imageStatus;
    private LocalDateTime imageLastCheckedAt;
    private String imageContentHash;

    public RecipeImageResponse() {}

    public RecipeImageResponse(Long recipeId, String title, String sourceRecipeId, String sourceUrl,
                               String imageUrl, String imageSourceUrl, ImageSourceType imageSourceType,
                               ImageStatus imageStatus, LocalDateTime imageLastCheckedAt, String imageContentHash) {
        this.recipeId = recipeId;
        this.title = title;
        this.sourceRecipeId = sourceRecipeId;
        this.sourceUrl = sourceUrl;
        this.imageUrl = imageUrl;
        this.imageSourceUrl = imageSourceUrl;
        this.imageSourceType = imageSourceType;
        this.imageStatus = imageStatus;
        this.imageLastCheckedAt = imageLastCheckedAt;
        this.imageContentHash = imageContentHash;
    }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceRecipeId() { return sourceRecipeId; }
    public void setSourceRecipeId(String sourceRecipeId) { this.sourceRecipeId = sourceRecipeId; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageSourceUrl() { return imageSourceUrl; }
    public void setImageSourceUrl(String imageSourceUrl) { this.imageSourceUrl = imageSourceUrl; }

    public ImageSourceType getImageSourceType() { return imageSourceType; }
    public void setImageSourceType(ImageSourceType imageSourceType) { this.imageSourceType = imageSourceType; }

    public ImageStatus getImageStatus() { return imageStatus; }
    public void setImageStatus(ImageStatus imageStatus) { this.imageStatus = imageStatus; }

    public LocalDateTime getImageLastCheckedAt() { return imageLastCheckedAt; }
    public void setImageLastCheckedAt(LocalDateTime imageLastCheckedAt) { this.imageLastCheckedAt = imageLastCheckedAt; }

    public String getImageContentHash() { return imageContentHash; }
    public void setImageContentHash(String imageContentHash) { this.imageContentHash = imageContentHash; }
}
