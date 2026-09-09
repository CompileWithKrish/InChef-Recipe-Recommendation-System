package com.inchef.dto.request;

import com.inchef.enums.MeasurementUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class UserIngredientRequest {

    private Long ingredientId;

    private String ingredientName;

    private String category;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    @NotNull(message = "Measurement unit is required")
    private MeasurementUnit unit;

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
