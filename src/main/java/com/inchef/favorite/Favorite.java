package com.inchef.favorite;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

import com.inchef.recipe.Recipe;
import com.inchef.user.User;

@Entity
@Table(name = "favorites", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_recipe_favorite", columnNames = {"user_id", "recipe_id"})
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @CreationTimestamp
    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;

    public Favorite() {}

    public Favorite(Long id, User user, Recipe recipe, LocalDateTime savedAt) {
        this.id = id;
        this.user = user;
        this.recipe = recipe;
        this.savedAt = savedAt;
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

    public Recipe getRecipe() {
        return this.recipe;
    }
    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public LocalDateTime getSavedAt() {
        return this.savedAt;
    }
    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private Recipe recipe;
        private LocalDateTime savedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder recipe(Recipe recipe) {
            this.recipe = recipe;
            return this;
        }

        public Builder savedAt(LocalDateTime savedAt) {
            this.savedAt = savedAt;
            return this;
        }

        public Favorite build() {
            Favorite obj = new Favorite();
            obj.setId(this.id);
            obj.setUser(this.user);
            obj.setRecipe(this.recipe);
            obj.setSavedAt(this.savedAt);
            return obj;
        }
    }
}
