package com.inchef.dto.request;

import com.inchef.enums.MeasurementUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RecipeIngredientRequest {

    private Long ingredientId;

    private String ingredientName; // Optional: auto-resolve or create if ingredientId not provided

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    @NotNull(message = "Measurement unit is required")
    private MeasurementUnit unit;

    private Boolean isOptional = false;

    private String notes;

    public RecipeIngredientRequest() {}

    public RecipeIngredientRequest(Long ingredientId, String ingredientName, Double quantity, MeasurementUnit unit, Boolean isOptional, String notes) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.isOptional = isOptional;
        this.notes = notes;
    }

    public Long getIngredientId() {
        return this.ingredientId;
    }
    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return this.ingredientName;
    }
    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public Double getQuantity() {
        return this.quantity;
    }
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public MeasurementUnit getUnit() {
        return this.unit;
    }
    public void setUnit(MeasurementUnit unit) {
        this.unit = unit;
    }

    public Boolean getIsOptional() {
        return this.isOptional;
    }
    public Boolean isIsOptional() {
        return this.isOptional;
    }
    public void setIsOptional(Boolean isOptional) {
        this.isOptional = isOptional;
    }

    public String getNotes() {
        return this.notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long ingredientId;
        private String ingredientName;
        private Double quantity;
        private MeasurementUnit unit;
        private Boolean isOptional = false;
        private String notes;

        public Builder ingredientId(Long ingredientId) {
            this.ingredientId = ingredientId;
            return this;
        }

        public Builder ingredientName(String ingredientName) {
            this.ingredientName = ingredientName;
            return this;
        }

        public Builder quantity(Double quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unit(MeasurementUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder isOptional(Boolean isOptional) {
            this.isOptional = isOptional;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public RecipeIngredientRequest build() {
            RecipeIngredientRequest obj = new RecipeIngredientRequest();
            obj.setIngredientId(this.ingredientId);
            obj.setIngredientName(this.ingredientName);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setIsOptional(this.isOptional);
            obj.setNotes(this.notes);
            return obj;
        }
    }
}
