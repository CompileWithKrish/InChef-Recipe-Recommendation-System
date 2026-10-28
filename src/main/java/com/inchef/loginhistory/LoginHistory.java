package com.inchef.loginhistory;

import jakarta.persistence.*;
import java.time.Instant;

import com.inchef.user.User;

@Entity
@Table(name = "login_history", indexes = {
        @Index(name = "idx_login_history_user_login_at", columnList = "user_id, login_at")
})
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_at", nullable = false, updatable = false)
    private Instant loginAt = Instant.now();

    @Column(name = "authentication_method", length = 50, nullable = false)
    private String authenticationMethod = "PASSWORD";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "SUCCESS";

    public LoginHistory() {}

    public LoginHistory(Long id, User user, Instant loginAt, String authenticationMethod, String status) {
        this.id = id;
        this.user = user;
        this.loginAt = loginAt != null ? loginAt : Instant.now();
        this.authenticationMethod = authenticationMethod != null ? authenticationMethod : "PASSWORD";
        this.status = status != null ? status : "SUCCESS";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Instant loginAt) {
        this.loginAt = loginAt;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private Instant loginAt = Instant.now();
        private String authenticationMethod = "PASSWORD";
        private String status = "SUCCESS";

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder loginAt(Instant loginAt) {
            this.loginAt = loginAt;
            return this;
        }

        public Builder authenticationMethod(String authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public LoginHistory build() {
            return new LoginHistory(id, user, loginAt, authenticationMethod, status);
        }
    }
}

