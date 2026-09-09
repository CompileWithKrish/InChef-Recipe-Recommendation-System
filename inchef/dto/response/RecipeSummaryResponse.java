package com.inchef.dto.response;

import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.enums.SpiceLevel;

import java.time.LocalDateTime;

public class RecipeSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private String cuisine;
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
    private Boolean isFavorite;
    private LocalDateTime createdAt;

    public RecipeSummaryResponse() {}

    public RecipeSummaryResponse(Long id, String title, String description, String cuisine, MealType mealType, DietaryType dietaryType, DifficultyLevel difficulty, Integer prepTimeMinutes, Integer cookTimeMinutes, Integer totalTimeMinutes, Integer servings, Integer calories, SpiceLevel spiceLevel, String imageUrl, Boolean isFavorite, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.cuisine = cuisine;
        this.mealType = mealType;
        this.dietaryType = dietaryType;
        this.difficulty = difficulty;
        this.prepTimeMinutes = prepTimeMinutes;
        this.cookTimeMinutes = cookTimeMinutes;
        this.totalTimeMinutes = totalTimeMinutes;
        this.servings = servings;
        this.calories = calories;
        this.spiceLevel = spiceLevel;
        this.imageUrl = imageUrl;
        this.isFavorite = isFavorite;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
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

    public MealType getMealType() {
        return this.mealType;
    }
    public void setMealType(MealType mealType) {
        this.mealType = mealType;
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
    public Boolean isIsFavorite() {
        return this.isFavorite;
    }
    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String description;
        private String cuisine;
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
        private Boolean isFavorite;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
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

        public Builder mealType(MealType mealType) {
            this.mealType = mealType;
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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecipeSummaryResponse build() {
            RecipeSummaryResponse obj = new RecipeSummaryResponse();
            obj.setId(this.id);
            obj.setTitle(this.title);
            obj.setDescription(this.description);
            obj.setCuisine(this.cuisine);
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
            obj.setIsFavorite(this.isFavorite);
            obj.setCreatedAt(this.createdAt);
            return obj;
        }
    }
}
