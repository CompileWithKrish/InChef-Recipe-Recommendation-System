package com.inchef.pantry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

import com.inchef.common.MeasurementUnit;

public class UserIngredientRequest {

    private Long ingredientId;

    private String ingredientName;

    private String category;

    @Positive(message = "Quantity must be positive")
    private Double quantity = 1.0;

    private MeasurementUnit unit = MeasurementUnit.PIECES;

    private LocalDate expiryDate;

    public UserIngredientRequest() {}

    public UserIngredientRequest(Long ingredientId, String ingredientName, String category, Double quantity, MeasurementUnit unit, LocalDate expiryDate) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
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
        return this.quantity != null ? this.quantity : 1.0;
    }
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public MeasurementUnit getUnit() {
        return this.unit != null ? this.unit : MeasurementUnit.PIECES;
    }
    public void setUnit(MeasurementUnit unit) {
        this.unit = unit != null ? unit : MeasurementUnit.PIECES;
    }

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long ingredientId;
        private String ingredientName;
        private String category;
        private Double quantity;
        private MeasurementUnit unit;
        private LocalDate expiryDate;

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

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public UserIngredientRequest build() {
            UserIngredientRequest obj = new UserIngredientRequest();
            obj.setIngredientId(this.ingredientId);
            obj.setIngredientName(this.ingredientName);
            obj.setCategory(this.category);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setExpiryDate(this.expiryDate);
            return obj;
        }
    }
}
