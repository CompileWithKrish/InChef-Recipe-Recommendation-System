package com.inchef.dto.response;

import com.inchef.enums.MeasurementUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserIngredientResponse {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private String category;
    private Double quantity;
    private MeasurementUnit unit;
    private LocalDate expiryDate;
    private String imageUrl;
    private LocalDateTime addedAt;

    public UserIngredientResponse() {}

    public UserIngredientResponse(Long id, Long ingredientId, String ingredientName, String category, Double quantity, MeasurementUnit unit, LocalDate expiryDate, String imageUrl, LocalDateTime addedAt) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.imageUrl = imageUrl;
        this.addedAt = addedAt;
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

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getAddedAt() {
        return this.addedAt;
    }
    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
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
        private LocalDate expiryDate;
        private String imageUrl;
        private LocalDateTime addedAt;

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

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder addedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public UserIngredientResponse build() {
            UserIngredientResponse obj = new UserIngredientResponse();
            obj.setId(this.id);
            obj.setIngredientId(this.ingredientId);
            obj.setIngredientName(this.ingredientName);
            obj.setCategory(this.category);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setExpiryDate(this.expiryDate);
            obj.setImageUrl(this.imageUrl);
            obj.setAddedAt(this.addedAt);
            return obj;
        }
    }
}
