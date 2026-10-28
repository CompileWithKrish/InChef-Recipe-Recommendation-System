package com.inchef.pantry;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.inchef.common.MeasurementUnit;
import com.inchef.ingredient.Ingredient;
import com.inchef.user.User;

@Entity
@Table(name = "user_ingredients", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_ingredient", columnNames = {"user_id", "ingredient_id"})
})
public class UserIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeasurementUnit unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    public UserIngredient() {}

    public UserIngredient(Long id, User user, Ingredient ingredient, Double quantity, MeasurementUnit unit, LocalDate expiryDate, LocalDateTime addedAt) {
        this.id = id;
        this.user = user;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.addedAt = addedAt;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return this.user;
    }
    public void setUser(User user) {
        this.user = user;
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

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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
        private User user;
        private Ingredient ingredient;
        private Double quantity;
        private MeasurementUnit unit;
        private LocalDate expiryDate;
        private LocalDateTime addedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
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

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder addedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public UserIngredient build() {
            UserIngredient obj = new UserIngredient();
            obj.setId(this.id);
            obj.setUser(this.user);
            obj.setIngredient(this.ingredient);
            obj.setQuantity(this.quantity);
            obj.setUnit(this.unit);
            obj.setExpiryDate(this.expiryDate);
            obj.setAddedAt(this.addedAt);
            return obj;
        }
    }
}
