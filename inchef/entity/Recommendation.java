package com.inchef.entity;

import com.inchef.enums.RecommendationMode;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_logs")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationMode mode;

    @Column(name = "input_query", columnDefinition = "TEXT")
    private String inputQuery;

    @Column(name = "recommended_count")
    private Integer recommendedRecipesCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Recommendation() {}

    public Recommendation(Long id, User user, RecommendationMode mode, String inputQuery, Integer recommendedRecipesCount, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.mode = mode;
        this.inputQuery = inputQuery;
        this.recommendedRecipesCount = recommendedRecipesCount;
        this.createdAt = createdAt;
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

    public RecommendationMode getMode() {
        return this.mode;
    }
    public void setMode(RecommendationMode mode) {
        this.mode = mode;
    }

    public String getInputQuery() {
        return this.inputQuery;
    }
    public void setInputQuery(String inputQuery) {
        this.inputQuery = inputQuery;
    }

    public Integer getRecommendedRecipesCount() {
        return this.recommendedRecipesCount;
    }
    public void setRecommendedRecipesCount(Integer recommendedRecipesCount) {
        this.recommendedRecipesCount = recommendedRecipesCount;
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
        private User user;
        private RecommendationMode mode;
        private String inputQuery;
        private Integer recommendedRecipesCount;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder mode(RecommendationMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder inputQuery(String inputQuery) {
            this.inputQuery = inputQuery;
            return this;
        }

        public Builder recommendedRecipesCount(Integer recommendedRecipesCount) {
            this.recommendedRecipesCount = recommendedRecipesCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Recommendation build() {
            Recommendation obj = new Recommendation();
            obj.setId(this.id);
            obj.setUser(this.user);
            obj.setMode(this.mode);
            obj.setInputQuery(this.inputQuery);
            obj.setRecommendedRecipesCount(this.recommendedRecipesCount);
            obj.setCreatedAt(this.createdAt);
            return obj;
        }
    }
}
