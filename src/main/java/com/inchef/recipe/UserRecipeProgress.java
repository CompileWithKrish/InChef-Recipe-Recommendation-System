package com.inchef.recipe;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_recipe_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
})
public class UserRecipeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "completed_step_indices", length = 1000)
    private String completedStepIndices; // Comma-separated 0-based indices e.g. "0,1,2"

    @Column(name = "total_steps")
    private Integer totalSteps = 0;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserRecipeProgress() {}

    public UserRecipeProgress(Long userId, Long recipeId, String completedStepIndices, Integer totalSteps, Boolean isCompleted) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.completedStepIndices = completedStepIndices;
        this.totalSteps = totalSteps;
        this.isCompleted = isCompleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getCompletedStepIndices() {
        return completedStepIndices;
    }

    public void setCompletedStepIndices(String completedStepIndices) {
        this.completedStepIndices = completedStepIndices;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
