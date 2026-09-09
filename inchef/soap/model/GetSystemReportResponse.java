package com.inchef.soap.model;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "totalUsers",
        "totalRecipes",
        "totalIngredients",
        "totalRecommendations",
        "cuisineStats",
        "topFavoritedRecipes",
        "generatedAt"
})
@XmlRootElement(name = "GetSystemReportResponse", namespace = "http://inchef.com/soap/reports")
public class GetSystemReportResponse {

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long totalUsers;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long totalRecipes;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long totalIngredients;

    @XmlElement(namespace = "http://inchef.com/soap/reports")
    private long totalRecommendations;

    @XmlElement(name = "cuisineStats", namespace = "http://inchef.com/soap/reports")

    private List<CuisineStat> cuisineStats = new ArrayList<>();

    @XmlElement(name = "topFavoritedRecipes", namespace = "http://inchef.com/soap/reports")

    private List<RecipePopularityStat> topFavoritedRecipes = new ArrayList<>();

    @XmlElement(namespace = "http://inchef.com/soap/reports", required = true)
    private String generatedAt;

    public GetSystemReportResponse() {}

    public GetSystemReportResponse(long totalUsers, long totalRecipes, long totalIngredients, long totalRecommendations, List<CuisineStat> cuisineStats, List<RecipePopularityStat> topFavoritedRecipes, String generatedAt) {
        this.totalUsers = totalUsers;
        this.totalRecipes = totalRecipes;
        this.totalIngredients = totalIngredients;
        this.totalRecommendations = totalRecommendations;
        this.cuisineStats = cuisineStats;
        this.topFavoritedRecipes = topFavoritedRecipes;
        this.generatedAt = generatedAt;
    }

    public long getTotalUsers() {
        return this.totalUsers;
    }
    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalRecipes() {
        return this.totalRecipes;
    }
    public void setTotalRecipes(long totalRecipes) {
        this.totalRecipes = totalRecipes;
    }

    public long getTotalIngredients() {
        return this.totalIngredients;
    }
    public void setTotalIngredients(long totalIngredients) {
        this.totalIngredients = totalIngredients;
    }

    public long getTotalRecommendations() {
        return this.totalRecommendations;
    }
    public void setTotalRecommendations(long totalRecommendations) {
        this.totalRecommendations = totalRecommendations;
    }

    public List<CuisineStat> getCuisineStats() {
        return this.cuisineStats;
    }
    public void setCuisineStats(List<CuisineStat> cuisineStats) {
        this.cuisineStats = cuisineStats;
    }

    public List<RecipePopularityStat> getTopFavoritedRecipes() {
        return this.topFavoritedRecipes;
    }
    public void setTopFavoritedRecipes(List<RecipePopularityStat> topFavoritedRecipes) {
        this.topFavoritedRecipes = topFavoritedRecipes;
    }

    public String getGeneratedAt() {
        return this.generatedAt;
    }
    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalUsers;
        private long totalRecipes;
        private long totalIngredients;
        private long totalRecommendations;
        private List<CuisineStat> cuisineStats = new ArrayList<>();
        private List<RecipePopularityStat> topFavoritedRecipes = new ArrayList<>();
        private String generatedAt;

        public Builder totalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        public Builder totalRecipes(long totalRecipes) {
            this.totalRecipes = totalRecipes;
            return this;
        }

        public Builder totalIngredients(long totalIngredients) {
            this.totalIngredients = totalIngredients;
            return this;
        }

        public Builder totalRecommendations(long totalRecommendations) {
            this.totalRecommendations = totalRecommendations;
            return this;
        }

        public Builder cuisineStats(List<CuisineStat> cuisineStats) {
            this.cuisineStats = cuisineStats;
            return this;
        }

        public Builder topFavoritedRecipes(List<RecipePopularityStat> topFavoritedRecipes) {
            this.topFavoritedRecipes = topFavoritedRecipes;
            return this;
        }

        public Builder generatedAt(String generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public GetSystemReportResponse build() {
            GetSystemReportResponse obj = new GetSystemReportResponse();
            obj.setTotalUsers(this.totalUsers);
            obj.setTotalRecipes(this.totalRecipes);
            obj.setTotalIngredients(this.totalIngredients);
            obj.setTotalRecommendations(this.totalRecommendations);
            obj.setCuisineStats(this.cuisineStats);
            obj.setTopFavoritedRecipes(this.topFavoritedRecipes);
            obj.setGeneratedAt(this.generatedAt);
            return obj;
        }
    }
}
