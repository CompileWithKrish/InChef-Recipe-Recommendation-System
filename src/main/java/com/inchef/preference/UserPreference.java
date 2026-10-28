package com.inchef.preference;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

import com.inchef.user.User;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", length = 30)
    private DietaryType dietaryType;

    @Column(name = "max_cooking_time")
    private Integer maxCookingTime; // in minutes

    @Column(name = "preferred_cuisines", length = 255)
    private String preferredCuisines; // e.g. "Italian, Mexican, Indian"

    @Column(length = 255)
    private String allergies; // e.g. "Peanuts, Dairy, Shellfish"

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_spice_level", length = 20)
    private SpiceLevel preferredSpiceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_skill_level", length = 30)
    private CookingSkillLevel cookingSkillLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserPreference() {}

    public UserPreference(Long id, User user, DietaryType dietaryType, Integer maxCookingTime, String preferredCuisines, String allergies, SpiceLevel preferredSpiceLevel, CookingSkillLevel cookingSkillLevel, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.dietaryType = dietaryType;
        this.maxCookingTime = maxCookingTime;
        this.preferredCuisines = preferredCuisines;
        this.allergies = allergies;
        this.preferredSpiceLevel = preferredSpiceLevel;
        this.cookingSkillLevel = cookingSkillLevel;
        this.updatedAt = updatedAt;
    }

    public UserPreference(Long id, User user, DietaryType dietaryType, Integer maxCookingTime, String preferredCuisines, String allergies, SpiceLevel preferredSpiceLevel, LocalDateTime updatedAt) {
        this(id, user, dietaryType, maxCookingTime, preferredCuisines, allergies, preferredSpiceLevel, null, updatedAt);
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

    public DietaryType getDietaryType() {
        return this.dietaryType;
    }
    public void setDietaryType(DietaryType dietaryType) {
        this.dietaryType = dietaryType;
    }

    public Integer getMaxCookingTime() {
        return this.maxCookingTime;
    }
    public void setMaxCookingTime(Integer maxCookingTime) {
        this.maxCookingTime = maxCookingTime;
    }

    public String getPreferredCuisines() {
        return this.preferredCuisines;
    }
    public void setPreferredCuisines(String preferredCuisines) {
        this.preferredCuisines = preferredCuisines;
    }

    public String getAllergies() {
        return this.allergies;
    }
    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public SpiceLevel getPreferredSpiceLevel() {
        return this.preferredSpiceLevel;
    }
    public void setPreferredSpiceLevel(SpiceLevel preferredSpiceLevel) {
        this.preferredSpiceLevel = preferredSpiceLevel;
    }

    public CookingSkillLevel getCookingSkillLevel() {
        return this.cookingSkillLevel;
    }
    public void setCookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
        this.cookingSkillLevel = cookingSkillLevel;
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
        private User user;
        private DietaryType dietaryType;
        private Integer maxCookingTime;
        private String preferredCuisines;
        private String allergies;
        private SpiceLevel preferredSpiceLevel;
        private CookingSkillLevel cookingSkillLevel;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder dietaryType(DietaryType dietaryType) {
            this.dietaryType = dietaryType;
            return this;
        }

        public Builder maxCookingTime(Integer maxCookingTime) {
            this.maxCookingTime = maxCookingTime;
            return this;
        }

        public Builder preferredCuisines(String preferredCuisines) {
            this.preferredCuisines = preferredCuisines;
            return this;
        }

        public Builder allergies(String allergies) {
            this.allergies = allergies;
            return this;
        }

        public Builder preferredSpiceLevel(SpiceLevel preferredSpiceLevel) {
            this.preferredSpiceLevel = preferredSpiceLevel;
            return this;
        }

        public Builder cookingSkillLevel(CookingSkillLevel cookingSkillLevel) {
            this.cookingSkillLevel = cookingSkillLevel;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserPreference build() {
            UserPreference obj = new UserPreference();
            obj.setId(this.id);
            obj.setUser(this.user);
            obj.setDietaryType(this.dietaryType);
            obj.setMaxCookingTime(this.maxCookingTime);
            obj.setPreferredCuisines(this.preferredCuisines);
            obj.setAllergies(this.allergies);
            obj.setPreferredSpiceLevel(this.preferredSpiceLevel);
            obj.setCookingSkillLevel(this.cookingSkillLevel);
            obj.setUpdatedAt(this.updatedAt);
            return obj;
        }
    }
}
