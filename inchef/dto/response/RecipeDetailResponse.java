package com.inchef.dto.response;

import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.enums.SpiceLevel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecipeDetailResponse {
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
    private String instructions;
    private String imageUrl;
    private String videoUrl;
    private Boolean isFavorite;
    private Long favoriteCount;

    private List<RecipeIngredientResponse> ingredients = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RecipeDetailResponse() {}

    public RecipeDetailResponse(Long id, String title, String description, String cuisine, MealType mealType, DietaryType dietaryType, DifficultyLevel difficulty, Integer prepTimeMinutes, Integer cookTimeMinutes, Integer totalTimeMinutes, Integer servings, Integer calories, SpiceLevel spiceLevel, String instructions, String imageUrl, String videoUrl, Boolean isFavorite, Long favoriteCount, List<RecipeIngredientResponse> ingredients, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.isFavorite = isFavorite;
        this.favoriteCount = favoriteCount;
        this.ingredients = ingredients;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getInstructions() {
        return this.instructions;
    }
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return this.videoUrl;
    }
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public Long getFavoriteCount() {
        return this.favoriteCount;
    }
    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public List<RecipeIngredientResponse> getIngredients() {
        return this.ingredients;
    }
    public void setIngredients(List<RecipeIngredientResponse> ingredients) {
        this.ingredients = ingredients;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        private String instructions;
        private String imageUrl;
        private String videoUrl;
        private Boolean isFavorite;
        private Long favoriteCount;
        private List<RecipeIngredientResponse> ingredients = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public Builder isFavorite(Boolean isFavorite) {
            this.isFavorite = isFavorite;
            return this;
        }

        public Builder favoriteCount(Long favoriteCount) {
            this.favoriteCount = favoriteCount;
            return this;
        }

        public Builder ingredients(List<RecipeIngredientResponse> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecipeDetailResponse build() {
            RecipeDetailResponse obj = new RecipeDetailResponse();
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
            obj.setInstructions(this.instructions);
            obj.setImageUrl(this.imageUrl);
            obj.setVideoUrl(this.videoUrl);
            obj.setIsFavorite(this.isFavorite);
            obj.setFavoriteCount(this.favoriteCount);
            obj.setIngredients(this.ingredients);
            obj.setCreatedAt(this.createdAt);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
