package com.inchef.soap.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecipePopularityStat", namespace = "http://inchef.com/soap/reports", propOrder = {
        "recipeId",
        "title",
        "cuisine",
        "favoriteCount"
})
public class RecipePopularityStat {

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long recipeId;

    @XmlElement(namespace = "http://inchef.com/soap/reports", required = true)
    private String title;

    @XmlElement(namespace = "http://inchef.com/soap/reports", required = true)
    private String cuisine;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long favoriteCount;

    public RecipePopularityStat() {}

    public RecipePopularityStat(long recipeId, String title, String cuisine, long favoriteCount) {
        this.recipeId = recipeId;
        this.title = title;
        this.cuisine = cuisine;
        this.favoriteCount = favoriteCount;
    }

    public long getRecipeId() {
        return this.recipeId;
    }
    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long recipeId;
        private String title;
        private String cuisine;
        private long favoriteCount;

        public Builder recipeId(long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
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

        public RecipePopularityStat build() {
            RecipePopularityStat obj = new RecipePopularityStat();
            obj.setRecipeId(this.recipeId);
            obj.setTitle(this.title);
            obj.setCuisine(this.cuisine);
            obj.setFavoriteCount(this.favoriteCount);
            return obj;
        }
    }
}
