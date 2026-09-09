package com.inchef.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserPreference preference;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private List<UserIngredient> pantryIngredients = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private List<Favorite> favorites = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long id, String email, String name, String phone, String avatarUrl, UserPreference preference, List<UserIngredient> pantryIngredients, List<Favorite> favorites, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.preference = preference;
        this.pantryIngredients = pantryIngredients;
        this.favorites = favorites;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserPreference getPreference() {
        return this.preference;
    }
    public void setPreference(UserPreference preference) {
        this.preference = preference;
    }

    public List<UserIngredient> getPantryIngredients() {
        return this.pantryIngredients;
    }
    public void setPantryIngredients(List<UserIngredient> pantryIngredients) {
        this.pantryIngredients = pantryIngredients;
    }

    public List<Favorite> getFavorites() {
        return this.favorites;
    }
    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String email;
        private String name;
        private String phone;
        private String avatarUrl;
        private UserPreference preference;
        private List<UserIngredient> pantryIngredients = new ArrayList<>();
        private List<Favorite> favorites = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Builder preference(UserPreference preference) {
            this.preference = preference;
            return this;
        }

        public Builder pantryIngredients(List<UserIngredient> pantryIngredients) {
            this.pantryIngredients = pantryIngredients;
            return this;
        }

        public Builder favorites(List<Favorite> favorites) {
            this.favorites = favorites;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            User obj = new User();
            obj.setId(this.id);
            obj.setEmail(this.email);
            obj.setName(this.name);
            obj.setPhone(this.phone);
            obj.setAvatarUrl(this.avatarUrl);
            obj.setPreference(this.preference);
            obj.setPantryIngredients(this.pantryIngredients);
            obj.setFavorites(this.favorites);
            obj.setCreatedAt(this.createdAt);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
