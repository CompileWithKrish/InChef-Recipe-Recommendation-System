package com.inchef.entity;

import com.inchef.enums.DietaryType;
import com.inchef.enums.DifficultyLevel;
import com.inchef.enums.MealType;
import com.inchef.enums.SpiceLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes", indexes = {
    @Index(name = "idx_recipe_title", columnList = "title"),
    @Index(name = "idx_recipe_cuisine", columnList = "cuisine"),
    @Index(name = "idx_recipe_dietary", columnList = "dietary_type"),
    @Index(name = "idx_recipe_meal_type", columnList = "meal_type")
})
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String cuisine; // e.g. Italian, Indian, Mexican, Chinese, American

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 30)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", length = 30)
    private DietaryType dietaryType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifficultyLevel difficulty;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    @Column(name = "total_time_minutes")
    private Integer totalTimeMinutes;

    private Integer servings;

    private Integer calories;

    @Enumerated(EnumType.STRING)
    @Column(name = "spice_level", length = 20)
    private SpiceLevel spiceLevel;

    @Column(columnDefinition = "TEXT")
    private String instructions; // Formatted step-by-step text or JSON array

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "is_published")

    private Boolean isPublished = true;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Recipe() {}

    public Recipe(Long id, String title, String description, String cuisine, MealType mealType, DietaryType dietaryType, DifficultyLevel difficulty, Integer prepTimeMinutes, Integer cookTimeMinutes, Integer totalTimeMinutes, Integer servings, Integer calories, SpiceLevel spiceLevel, String instructions, String imageUrl, String videoUrl, Boolean isPublished, List<RecipeIngredient> recipeIngredients, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.isPublished = isPublished;
        this.recipeIngredients = recipeIngredients;
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

    public Boolean getIsPublished() {
        return this.isPublished;
    }
    public Boolean isIsPublished() {
        return this.isPublished;
    }
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public List<RecipeIngredient> getRecipeIngredients() {
        return this.recipeIngredients;
    }
    public void setRecipeIngredients(List<RecipeIngredient> recipeIngredients) {
        this.recipeIngredients = recipeIngredients;
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

    public void addRecipeIngredient(RecipeIngredient recipeIngredient) {
        recipeIngredients.add(recipeIngredient);
        recipeIngredient.setRecipe(this);
    }

    public void removeRecipeIngredient(RecipeIngredient recipeIngredient) {
        recipeIngredients.remove(recipeIngredient);
        recipeIngredient.setRecipe(null);
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
        private Boolean isPublished = true;
        private List<RecipeIngredient> recipeIngredients = new ArrayList<>();
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

        public Builder isPublished(Boolean isPublished) {
            this.isPublished = isPublished;
            return this;
        }

        public Builder recipeIngredients(List<RecipeIngredient> recipeIngredients) {
            this.recipeIngredients = recipeIngredients;
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

        public Recipe build() {
            Recipe obj = new Recipe();
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
            obj.setIsPublished(this.isPublished);
            obj.setRecipeIngredients(this.recipeIngredients);
            obj.setCreatedAt(this.createdAt);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
