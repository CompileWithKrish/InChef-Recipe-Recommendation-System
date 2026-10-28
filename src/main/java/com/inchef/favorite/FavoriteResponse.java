package com.inchef.favorite;

import java.time.LocalDateTime;

import com.inchef.preference.DietaryType;
import com.inchef.preference.SpiceLevel;
import com.inchef.recipe.DifficultyLevel;
import com.inchef.recipe.MealType;
import com.inchef.recipe.RecipeSummaryResponse;

public class FavoriteResponse {
    private Long id;
    private Long favoriteId;
    private Long userId;
    private Long recipeId;
    private String title;
    private String translatedRecipeName;
    private String description;
    private String cuisine;
    private String course;
    private MealType mealType;
    private DietaryType dietaryType;
    private DifficultyLevel difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer totalTimeMinutes;
    private Integer servings;
    private Integer calories;
    private SpiceLevel spiceLevel;
    private String imageUrl;
    private Boolean isFavorite = true;
    private RecipeSummaryResponse recipe;
    private LocalDateTime savedAt;

    public FavoriteResponse() {}

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
        this.favoriteId = id;
    }

    public Long getFavoriteId() {
        return this.favoriteId != null ? this.favoriteId : this.id;
    }

    public void setFavoriteId(Long favoriteId) {
        this.favoriteId = favoriteId;
        if (this.id == null) {
            this.id = favoriteId;
        }
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecipeId() {
        return this.recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranslatedRecipeName() {
        return this.translatedRecipeName;
    }

    public void setTranslatedRecipeName(String translatedRecipeName) {
        this.translatedRecipeName = translatedRecipeName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCuisine() {
        return this.cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getCourse() {
        return this.course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public MealType getMealType() {
        return this.mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
        if (this.course == null && mealType != null) {
            this.course = mealType.name();
        }
    }

    public DietaryType getDietaryType() {
        return this.dietaryType;
    }

    public void setDietaryType(DietaryType dietaryType) {
        this.dietaryType = dietaryType;
    }

    public DifficultyLevel getDifficulty() {
        return this.difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getPrepTimeMinutes() {
        return this.prepTimeMinutes;
    }

    public void setPrepTimeMinutes(Integer prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public Integer getCookTimeMinutes() {
        return this.cookTimeMinutes;
    }

    public void setCookTimeMinutes(Integer cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public Integer getTotalTimeMinutes() {
        return this.totalTimeMinutes;
    }

    public void setTotalTimeMinutes(Integer totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public Integer getServings() {
        return this.servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public Integer getCalories() {
        return this.calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public SpiceLevel getSpiceLevel() {
        return this.spiceLevel;
    }

    public void setSpiceLevel(SpiceLevel spiceLevel) {
        this.spiceLevel = spiceLevel;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsFavorite() {
        return this.isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
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
        private Long favoriteId;
        private Long userId;
        private Long recipeId;
        private String title;
        private String translatedRecipeName;
        private String description;
        private String cuisine;
        private String course;
        private MealType mealType;
        private DietaryType dietaryType;
        private DifficultyLevel difficulty;
        private Integer prepTimeMinutes;
        private Integer cookTimeMinutes;
        private Integer totalTimeMinutes;
        private Integer servings;
        private Integer calories;
        private SpiceLevel spiceLevel;
        private String imageUrl;
        private Boolean isFavorite = true;
        private RecipeSummaryResponse recipe;
        private LocalDateTime savedAt;

        public Builder id(Long id) {
            this.id = id;
            this.favoriteId = id;
            return this;
        }

        public Builder favoriteId(Long favoriteId) {
            this.favoriteId = favoriteId;
            if (this.id == null) {
                this.id = favoriteId;
            }
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder recipeId(Long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder translatedRecipeName(String translatedRecipeName) {
            this.translatedRecipeName = translatedRecipeName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder cuisine(String cuisine) {
            this.cuisine = cuisine;
            return this;
        }

        public Builder course(String course) {
            this.course = course;
            return this;
        }

        public Builder mealType(MealType mealType) {
            this.mealType = mealType;
            if (this.course == null && mealType != null) {
                this.course = mealType.name();
            }
            return this;
        }

        public Builder dietaryType(DietaryType dietaryType) {
            this.dietaryType = dietaryType;
            return this;
        }

        public Builder difficulty(DifficultyLevel difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder prepTimeMinutes(Integer prepTimeMinutes) {
            this.prepTimeMinutes = prepTimeMinutes;
            return this;
        }

        public Builder cookTimeMinutes(Integer cookTimeMinutes) {
            this.cookTimeMinutes = cookTimeMinutes;
            return this;
        }

        public Builder totalTimeMinutes(Integer totalTimeMinutes) {
            this.totalTimeMinutes = totalTimeMinutes;
            return this;
        }

        public Builder servings(Integer servings) {
            this.servings = servings;
            return this;
        }

        public Builder calories(Integer calories) {
            this.calories = calories;
            return this;
        }

        public Builder spiceLevel(SpiceLevel spiceLevel) {
            this.spiceLevel = spiceLevel;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder isFavorite(Boolean isFavorite) {
            this.isFavorite = isFavorite;
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
            obj.setFavoriteId(this.favoriteId != null ? this.favoriteId : this.id);
            obj.setUserId(this.userId);
            obj.setRecipeId(this.recipeId);
            obj.setTitle(this.title);
            obj.setTranslatedRecipeName(this.translatedRecipeName);
            obj.setDescription(this.description);
            obj.setCuisine(this.cuisine);
            obj.setCourse(this.course);
            obj.setMealType(this.mealType);
            obj.setDietaryType(this.dietaryType);
            obj.setDifficulty(this.difficulty);
            obj.setPrepTimeMinutes(this.prepTimeMinutes);
            obj.setCookTimeMinutes(this.cookTimeMinutes);
            obj.setTotalTimeMinutes(this.totalTimeMinutes);
            obj.setServings(this.servings);
            obj.setCalories(this.calories);
            obj.setSpiceLevel(this.spiceLevel);
            obj.setImageUrl(this.imageUrl);
            obj.setIsFavorite(this.isFavorite != null ? this.isFavorite : true);
            obj.setRecipe(this.recipe);
            obj.setSavedAt(this.savedAt);
            return obj;
        }
    }
}
