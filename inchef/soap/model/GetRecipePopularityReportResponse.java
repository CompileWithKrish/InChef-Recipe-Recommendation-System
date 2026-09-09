package com.inchef.soap.model;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "recipeId",
        "recipeTitle",
        "cuisine",
        "favoriteCount",
        "ingredientCount",
        "isPublished"
})
@XmlRootElement(name = "GetRecipePopularityReportResponse", namespace = "http://inchef.com/soap/reports")
public class GetRecipePopularityReportResponse {

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long recipeId;

    @XmlElement(namespace = "http://inchef.com/soap/reports", required = true)
    private String recipeTitle;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private String cuisine;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long favoriteCount;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private int ingredientCount;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private boolean isPublished;

    public GetRecipePopularityReportResponse() {}

    public GetRecipePopularityReportResponse(long recipeId, String recipeTitle, String cuisine, long favoriteCount, int ingredientCount, boolean isPublished) {
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.cuisine = cuisine;
        this.favoriteCount = favoriteCount;
        this.ingredientCount = ingredientCount;
        this.isPublished = isPublished;
    }

    public long getRecipeId() {
        return this.recipeId;
    }
    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeTitle() {
        return this.recipeTitle;
    }
    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getCuisine() {
        return this.cuisine;
    }
    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public long getFavoriteCount() {
        return this.favoriteCount;
    }
    public void setFavoriteCount(long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public int getIngredientCount() {
        return this.ingredientCount;
    }
    public void setIngredientCount(int ingredientCount) {
        this.ingredientCount = ingredientCount;
    }

    public boolean isIsPublished() {
        return this.isPublished;
    }
    public void setIsPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long recipeId;
        private String recipeTitle;
        private String cuisine;
        private long favoriteCount;
        private int ingredientCount;
        private boolean isPublished;

        public Builder recipeId(long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public Builder recipeTitle(String recipeTitle) {
            this.recipeTitle = recipeTitle;
            return this;
        }

        public Builder cuisine(String cuisine) {
            this.cuisine = cuisine;
            return this;
        }

        public Builder favoriteCount(long favoriteCount) {
            this.favoriteCount = favoriteCount;
            return this;
        }

        public Builder ingredientCount(int ingredientCount) {
            this.ingredientCount = ingredientCount;
            return this;
        }

        public Builder isPublished(boolean isPublished) {
            this.isPublished = isPublished;
            return this;
        }

        public GetRecipePopularityReportResponse build() {
            GetRecipePopularityReportResponse obj = new GetRecipePopularityReportResponse();
            obj.setRecipeId(this.recipeId);
            obj.setRecipeTitle(this.recipeTitle);
            obj.setCuisine(this.cuisine);
            obj.setFavoriteCount(this.favoriteCount);
            obj.setIngredientCount(this.ingredientCount);
            obj.setIsPublished(this.isPublished);
            return obj;
        }
    }
}
