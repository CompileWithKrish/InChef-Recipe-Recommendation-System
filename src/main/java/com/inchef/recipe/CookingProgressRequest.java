package com.inchef.recipe;

import java.util.List;

public class CookingProgressRequest {

    private Long userId;
    private Integer stepIndex;
    private Boolean completed;
    private List<Integer> completedStepIndices;
    private Integer totalSteps;

    public CookingProgressRequest() {}

    public CookingProgressRequest(Long userId, Integer stepIndex, Boolean completed, List<Integer> completedStepIndices, Integer totalSteps) {
        this.userId = userId;
        this.stepIndex = stepIndex;
        this.completed = completed;
        this.completedStepIndices = completedStepIndices;
        this.totalSteps = totalSteps;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public List<Integer> getCompletedStepIndices() {
        return completedStepIndices;
    }

    public void setCompletedStepIndices(List<Integer> completedStepIndices) {
        this.completedStepIndices = completedStepIndices;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }
}
