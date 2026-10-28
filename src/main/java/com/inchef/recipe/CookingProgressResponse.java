package com.inchef.recipe;

import java.time.LocalDateTime;
import java.util.List;

public class CookingProgressResponse {

    private Long recipeId;
    private Long userId;
    private List<Integer> completedStepIndices;
    private int completedStepsCount;
    private int totalSteps;
    private double progressPercentage;
    private boolean isCompleted;
    private LocalDateTime updatedAt;

    public CookingProgressResponse() {}

    public CookingProgressResponse(Long recipeId, Long userId, List<Integer> completedStepIndices, int completedStepsCount, int totalSteps, double progressPercentage, boolean isCompleted, LocalDateTime updatedAt) {
        this.recipeId = recipeId;
        this.userId = userId;
        this.completedStepIndices = completedStepIndices;
        this.completedStepsCount = completedStepsCount;
        this.totalSteps = totalSteps;
        this.progressPercentage = progressPercentage;
        this.isCompleted = isCompleted;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Integer> getCompletedStepIndices() {
        return completedStepIndices;
    }

    public void setCompletedStepIndices(List<Integer> completedStepIndices) {
        this.completedStepIndices = completedStepIndices;
    }

    public int getCompletedStepsCount() {
        return completedStepsCount;
    }

    public void setCompletedStepsCount(int completedStepsCount) {
        this.completedStepsCount = completedStepsCount;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Builder {
        private Long recipeId;
        private Long userId;
        private List<Integer> completedStepIndices;
        private int completedStepsCount;
        private int totalSteps;
        private double progressPercentage;
        private boolean isCompleted;
        private LocalDateTime updatedAt;

        public Builder recipeId(Long recipeId) {
            this.recipeId = recipeId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder completedStepIndices(List<Integer> completedStepIndices) {
            this.completedStepIndices = completedStepIndices;
            return this;
        }

        public Builder completedStepsCount(int completedStepsCount) {
            this.completedStepsCount = completedStepsCount;
            return this;
        }

        public Builder totalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
            return this;
        }

        public Builder progressPercentage(double progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public Builder isCompleted(boolean isCompleted) {
            this.isCompleted = isCompleted;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CookingProgressResponse build() {
            return new CookingProgressResponse(recipeId, userId, completedStepIndices, completedStepsCount, totalSteps, progressPercentage, isCompleted, updatedAt);
        }
    }
}
