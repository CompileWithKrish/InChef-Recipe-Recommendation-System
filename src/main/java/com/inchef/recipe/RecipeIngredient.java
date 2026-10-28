package com.inchef.recipe;

import jakarta.persistence.*;

import com.inchef.common.MeasurementUnit;
import com.inchef.ingredient.Ingredient;

@Entity
@Table(name = "recipe_ingredients", uniqueConstraints = {
    @UniqueConstraint(name = "uk_recipe_ingredient", columnNames = {"recipe_id", "ingredient_id"})
})
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeasurementUnit unit;

    @Column(name = "is_optional")

    private Boolean isOptional = false;

    @Column(length = 255)
    private String notes;

    public RecipeIngredient() {}

    public RecipeIngredient(Long id, Recipe recipe, Ingredient ingredient, Double quantity, MeasurementUnit unit, Boolean isOptional, String notes) {
        this.id = id;
        this.recipe = recipe;
        this.ingredient = ingredient;
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

    public Recipe getRecipe() {
        return this.recipe;
    }
    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }
    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
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
        private Recipe recipe;
        private Ingredient ingredient;
        private Double quantity;
        private MeasurementUnit unit;
        private Boolean isOptional = false;
        private String notes;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder recipe(Recipe recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder ingredient(Ingredient ingredient) {
            this.ingredient = ingredient;
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

        public RecipeIngredient build() {
            RecipeIngredient obj = new RecipeIngredient();
            obj.setId(this.id);
            obj.setRecipe(this.recipe);
            obj.setIngredient(this.ingredient);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setIsOptional(this.isOptional);
            obj.setNotes(this.notes);
            return obj;
        }
    }
}
