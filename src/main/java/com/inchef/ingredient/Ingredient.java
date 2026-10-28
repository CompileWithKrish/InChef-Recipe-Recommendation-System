package com.inchef.ingredient;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

import com.inchef.common.MeasurementUnit;

@Entity
@Table(name = "ingredients", indexes = {
    @Index(name = "idx_ingredient_name", columnList = "name"),
    @Index(name = "idx_ingredient_category", columnList = "category")
})
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category; // e.g. Vegetables, Spices, Dairy, Grains, Protein, Condiments, Oils

    @Enumerated(EnumType.STRING)
    @Column(name = "default_unit", length = 30)
    private MeasurementUnit defaultUnit;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Ingredient() {}

    public Ingredient(Long id, String name, String category, MeasurementUnit defaultUnit, String imageUrl, LocalDateTime createdAt) {
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

        public Ingredient build() {
            Ingredient obj = new Ingredient();
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
