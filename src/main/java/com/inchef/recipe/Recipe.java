package com.inchef.recipe;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.inchef.preference.DietaryType;
import com.inchef.preference.SpiceLevel;
import com.inchef.recipeimage.ImageSourceType;
import com.inchef.recipeimage.ImageStatus;

@Entity
@Table(name = "recipes", indexes = {
    @Index(name = "idx_recipe_title", columnList = "title"),
    @Index(name = "idx_recipe_cuisine", columnList = "cuisine"),
    @Index(name = "idx_recipe_dietary", columnList = "dietary_type"),
    @Index(name = "idx_recipe_meal_type", columnList = "meal_type"),
    @Index(name = "idx_recipe_image_status", columnList = "image_status"),
    @Index(name = "idx_recipe_source_recipe_id", columnList = "source_recipe_id")
})
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "translated_recipe_name", length = 200)
    private String translatedRecipeName;

    @Column(name = "ingredient_text", columnDefinition = "TEXT")
    private String ingredientText;

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

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "image_source_url", length = 500)
    private String imageSourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_source_type", length = 50)
    private ImageSourceType imageSourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_status", length = 50)
    private ImageStatus imageStatus = ImageStatus.PENDING;

    @Column(name = "image_last_checked_at")
    private LocalDateTime imageLastCheckedAt;

    @Column(name = "image_content_hash", length = 100)
    private String imageContentHash;

    @Column(name = "source_dataset", length = 50)
    private String sourceDataset;

    @Column(name = "source_recipe_id", length = 100)
    private String sourceRecipeId;

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

    public String getTranslatedRecipeName() {
        return this.translatedRecipeName;
    }
    public void setTranslatedRecipeName(String translatedRecipeName) {
        this.translatedRecipeName = translatedRecipeName;
    }

    public String getIngredientText() {
        return this.ingredientText;
    }
    public void setIngredientText(String ingredientText) {
        this.ingredientText = ingredientText;
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

    public String getSourceUrl() {
        return this.sourceUrl;
    }
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceDataset() {
        return this.sourceDataset;
    }
    public void setSourceDataset(String sourceDataset) {
        this.sourceDataset = sourceDataset;
    }

    public String getSourceRecipeId() {
        return this.sourceRecipeId;
    }
    public void setSourceRecipeId(String sourceRecipeId) {
        this.sourceRecipeId = sourceRecipeId;
    }

    public String getImageSourceUrl() {
        return this.imageSourceUrl;
    }
    public void setImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
    }

    public ImageSourceType getImageSourceType() {
        return this.imageSourceType;
    }
    public void setImageSourceType(ImageSourceType imageSourceType) {
        this.imageSourceType = imageSourceType;
    }

    public ImageStatus getImageStatus() {
        return this.imageStatus;
    }
    public void setImageStatus(ImageStatus imageStatus) {
        this.imageStatus = imageStatus;
    }

    public LocalDateTime getImageLastCheckedAt() {
        return this.imageLastCheckedAt;
    }
    public void setImageLastCheckedAt(LocalDateTime imageLastCheckedAt) {
        this.imageLastCheckedAt = imageLastCheckedAt;
    }

    public String getImageContentHash() {
        return this.imageContentHash;
    }
    public void setImageContentHash(String imageContentHash) {
        this.imageContentHash = imageContentHash;
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
        private String translatedRecipeName;
        private String ingredientText;
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

        public Builder translatedRecipeName(String translatedRecipeName) {
            this.translatedRecipeName = translatedRecipeName;
            return this;
        }

        public Builder ingredientText(String ingredientText) {
            this.ingredientText = ingredientText;
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

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder sourceDataset(String sourceDataset) {
            this.sourceDataset = sourceDataset;
            return this;
        }

        public Builder sourceRecipeId(String sourceRecipeId) {
            this.sourceRecipeId = sourceRecipeId;
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

        private String sourceUrl;
        private String sourceDataset;
        private String sourceRecipeId;
        private String imageSourceUrl;
        private ImageSourceType imageSourceType;
        private ImageStatus imageStatus;
        private LocalDateTime imageLastCheckedAt;
        private String imageContentHash;

        public Builder imageSourceUrl(String imageSourceUrl) {
            this.imageSourceUrl = imageSourceUrl;
            return this;
        }

        public Builder imageSourceType(ImageSourceType imageSourceType) {
            this.imageSourceType = imageSourceType;
            return this;
        }

        public Builder imageStatus(ImageStatus imageStatus) {
            this.imageStatus = imageStatus;
            return this;
        }

        public Builder imageLastCheckedAt(LocalDateTime imageLastCheckedAt) {
            this.imageLastCheckedAt = imageLastCheckedAt;
            return this;
        }

        public Builder imageContentHash(String imageContentHash) {
            this.imageContentHash = imageContentHash;
            return this;
        }

        public Recipe build() {
            Recipe obj = new Recipe();
            obj.setId(this.id);
            obj.setTitle(this.title);
            obj.setTranslatedRecipeName(this.translatedRecipeName);
            obj.setIngredientText(this.ingredientText);
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
            obj.setSourceUrl(this.sourceUrl);
            obj.setImageSourceUrl(this.imageSourceUrl);
            obj.setImageSourceType(this.imageSourceType);
            obj.setImageStatus(this.imageStatus != null ? this.imageStatus : ImageStatus.PENDING);
            obj.setImageLastCheckedAt(this.imageLastCheckedAt);
            obj.setImageContentHash(this.imageContentHash);
            obj.setSourceDataset(this.sourceDataset);
            obj.setSourceRecipeId(this.sourceRecipeId);
            obj.setIsPublished(this.isPublished);
            obj.setRecipeIngredients(this.recipeIngredients);
            obj.setCreatedAt(this.createdAt);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
