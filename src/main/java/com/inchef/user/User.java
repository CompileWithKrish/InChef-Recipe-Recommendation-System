package com.inchef.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.inchef.favorite.Favorite;
import com.inchef.loginhistory.LoginHistory;
import com.inchef.pantry.UserIngredient;
import com.inchef.preference.UserPreference;
import com.inchef.recommendation.Recommendation;

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

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "auth_provider", length = 30)
    private String authProvider = "LOCAL";

    @Column(name = "google_id", length = 100, unique = true)
    private String googleId;

    @Column(name = "email_verified")
    private Boolean emailVerified = true;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserPreference preference;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)

    private List<UserIngredient> pantryIngredients = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Favorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Recommendation> recommendations = new ArrayList<>();
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoginHistory> loginHistories = new ArrayList<>();

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

    public String getPasswordHash() {
        return this.passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAuthProvider() {
        return this.authProvider;
    }
    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleId() {
        return this.googleId;
    }
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public Boolean getEmailVerified() {
        return this.emailVerified;
    }
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Integer getFailedLoginAttempts() {
        return this.failedLoginAttempts != null ? this.failedLoginAttempts : 0;
    }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockoutUntil() {
        return this.lockoutUntil;
    }
    public void setLockoutUntil(LocalDateTime lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
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

    public List<Recommendation> getRecommendations() {
        return this.recommendations;
    }
    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public List<LoginHistory> getLoginHistories() {
        return this.loginHistories;
    }
    public void setLoginHistories(List<LoginHistory> loginHistories) {
        this.loginHistories = loginHistories;
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
        private String passwordHash;
        private String authProvider;
        private String googleId;
        private Boolean emailVerified;
        private Integer failedLoginAttempts;
        private LocalDateTime lockoutUntil;
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

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder authProvider(String authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder googleId(String googleId) {
            this.googleId = googleId;
            return this;
        }

        public Builder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder failedLoginAttempts(Integer failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder lockoutUntil(LocalDateTime lockoutUntil) {
            this.lockoutUntil = lockoutUntil;
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
            obj.setPasswordHash(this.passwordHash);
            if (this.authProvider != null) obj.setAuthProvider(this.authProvider);
            obj.setGoogleId(this.googleId);
            if (this.emailVerified != null) obj.setEmailVerified(this.emailVerified);
            if (this.failedLoginAttempts != null) obj.setFailedLoginAttempts(this.failedLoginAttempts);
            obj.setLockoutUntil(this.lockoutUntil);
            obj.setPreference(this.preference);
            obj.setPantryIngredients(this.pantryIngredients);
            obj.setFavorites(this.favorites);
            obj.setCreatedAt(this.createdAt);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
