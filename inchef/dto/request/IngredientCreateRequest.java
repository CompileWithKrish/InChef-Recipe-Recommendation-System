package com.inchef.dto.request;

import com.inchef.enums.MeasurementUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IngredientCreateRequest {

    @NotBlank(message = "Ingredient name is required")
    @Size(max = 100, message = "Ingredient name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    @NotNull(message = "Default measurement unit is required")
    private MeasurementUnit defaultUnit;

    private String imageUrl;

    public IngredientCreateRequest() {}

    public IngredientCreateRequest(String name, String category, MeasurementUnit defaultUnit, String imageUrl) {
        this.name = name;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.imageUrl = imageUrl;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String category;
        private MeasurementUnit defaultUnit;
        private String imageUrl;

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

        public IngredientCreateRequest build() {
            IngredientCreateRequest obj = new IngredientCreateRequest();
            obj.setName(this.name);
            obj.setCategory(this.category);
            obj.setDefaultUnit(this.defaultUnit);
            obj.setImageUrl(this.imageUrl);
            return obj;
        }
    }
}
