package com.inchef.dto.request;

import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.enums.SpiceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

public class RecipeCreateRequest {

    @NotBlank(message = "Recipe title is required")
    private String title;

    private String description;

    private String cuisine;

    @NotNull(message = "Meal type is required")
    private MealType mealType;

    @NotNull(message = "Dietary type is required")
    private DietaryType dietaryType;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficulty;

    @Positive(message = "Prep time must be positive")
    private Integer prepTimeMinutes;

    @Positive(message = "Cook time must be positive")
    private Integer cookTimeMinutes;

    private Integer totalTimeMinutes;

    @Positive(message = "Servings must be positive")
    private Integer servings;

    private Integer calories;

    private SpiceLevel spiceLevel;

    @NotBlank(message = "Instructions are required")
    private String instructions;

    private String imageUrl;

    private String videoUrl;

    private Boolean isPublished = true;

    @NotEmpty(message = "At least one recipe ingredient is required")
    @Valid

    private List<RecipeIngredientRequest> ingredients = new ArrayList<>();

    public RecipeCreateRequest() {}

    public RecipeCreateRequest(String title, String description, String cuisine, MealType mealType, DietaryType dietaryType, DifficultyLevel difficulty, Integer prepTimeMinutes, Integer cookTimeMinutes, Integer totalTimeMinutes, Integer servings, Integer calories, SpiceLevel spiceLevel, String instructions, String imageUrl, String videoUrl, Boolean isPublished, List<RecipeIngredientRequest> ingredients) {
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
        this.isPublished = isPublished;
        this.ingredients = ingredients;
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

    public Boolean getIsPublished() {
        return this.isPublished;
    }
    public Boolean isIsPublished() {
        return this.isPublished;
    }
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public List<RecipeIngredientRequest> getIngredients() {
        return this.ingredients;
    }
    public void setIngredients(List<RecipeIngredientRequest> ingredients) {
        this.ingredients = ingredients;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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
        private Boolean isPublished = true;
        private List<RecipeIngredientRequest> ingredients = new ArrayList<>();

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

        public Builder isPublished(Boolean isPublished) {
            this.isPublished = isPublished;
            return this;
        }

        public Builder ingredients(List<RecipeIngredientRequest> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public RecipeCreateRequest build() {
            RecipeCreateRequest obj = new RecipeCreateRequest();
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
            obj.setIsPublished(this.isPublished);
            obj.setIngredients(this.ingredients);
            return obj;
        }
    }
}
