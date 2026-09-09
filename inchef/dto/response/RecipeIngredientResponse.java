package com.inchef.dto.response;

import com.inchef.enums.MeasurementUnit;

public class RecipeIngredientResponse {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private String category;
    private Double quantity;
    private MeasurementUnit unit;
    private Boolean isOptional;
    private String notes;

    public RecipeIngredientResponse() {}

    public RecipeIngredientResponse(Long id, Long ingredientId, String ingredientName, String category, Double quantity, MeasurementUnit unit, Boolean isOptional, String notes) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.isOptional = isOptional;
        this.notes = notes;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
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

    public String getCategory() {
        return this.category;
    }
    public void setCategory(String category) {
        this.category = category;
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
        private Long id;
        private Long ingredientId;
        private String ingredientName;
        private String category;
        private Double quantity;
        private MeasurementUnit unit;
        private Boolean isOptional;
        private String notes;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder ingredientId(Long ingredientId) {
            this.ingredientId = ingredientId;
            return this;
        }

        public Builder ingredientName(String ingredientName) {
            this.ingredientName = ingredientName;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
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

        public RecipeIngredientResponse build() {
            RecipeIngredientResponse obj = new RecipeIngredientResponse();
            obj.setId(this.id);
            obj.setIngredientId(this.ingredientId);
            obj.setIngredientName(this.ingredientName);
            obj.setCategory(this.category);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setIsOptional(this.isOptional);
            obj.setNotes(this.notes);
            return obj;
        }
    }
}
