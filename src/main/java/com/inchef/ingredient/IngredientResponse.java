package com.inchef.ingredient;

import java.time.LocalDateTime;

import com.inchef.common.MeasurementUnit;

public class IngredientResponse {
    private Long id;
    private String name;
    private String category;
    private MeasurementUnit defaultUnit;
    private String imageUrl;
    private LocalDateTime createdAt;

    public IngredientResponse() {}

    public IngredientResponse(Long id, String name, String category, MeasurementUnit defaultUnit, String imageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return this.category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public MeasurementUnit getDefaultUnit() {
        return this.defaultUnit;
    }
    public void setDefaultUnit(MeasurementUnit defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String category;
        private MeasurementUnit defaultUnit;
        private String imageUrl;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder defaultUnit(MeasurementUnit defaultUnit) {
            this.defaultUnit = defaultUnit;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IngredientResponse build() {
            IngredientResponse obj = new IngredientResponse();
            obj.setId(this.id);
            obj.setName(this.name);
            obj.setCategory(this.category);
            obj.setDefaultUnit(this.defaultUnit);
            obj.setImageUrl(this.imageUrl);
            obj.setCreatedAt(this.createdAt);
            return obj;
        }
    }
}
