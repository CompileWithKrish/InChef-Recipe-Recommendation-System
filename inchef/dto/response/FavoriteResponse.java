package com.inchef.dto.response;


import java.time.LocalDateTime;

public class FavoriteResponse {
    private Long id;
    private Long userId;
    private RecipeSummaryResponse recipe;
    private LocalDateTime savedAt;

    public FavoriteResponse() {}

    public FavoriteResponse(Long id, Long userId, RecipeSummaryResponse recipe, LocalDateTime savedAt) {
        this.id = id;
        this.userId = userId;
        this.recipe = recipe;
        this.savedAt = savedAt;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return this.userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RecipeSummaryResponse getRecipe() {
        return this.recipe;
    }
    public void setRecipe(RecipeSummaryResponse recipe) {
        this.recipe = recipe;
    }

    public LocalDateTime getSavedAt() {
        return this.savedAt;
    }
    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private RecipeSummaryResponse recipe;
        private LocalDateTime savedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder recipe(RecipeSummaryResponse recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder savedAt(LocalDateTime savedAt) {
            this.savedAt = savedAt;
            return this;
        }

        public FavoriteResponse build() {
            FavoriteResponse obj = new FavoriteResponse();
            obj.setId(this.id);
            obj.setUserId(this.userId);
            obj.setRecipe(this.recipe);
            obj.setSavedAt(this.savedAt);
            return obj;
        }
    }
}
