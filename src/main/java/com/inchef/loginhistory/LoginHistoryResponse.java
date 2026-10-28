package com.inchef.loginhistory;

import java.time.Instant;

public class LoginHistoryResponse {

    private Long id;
    private Instant loginAt;
    private String timeZone;
    private String authenticationMethod;
    private String status;

    public LoginHistoryResponse() {}

    public LoginHistoryResponse(Long id, Instant loginAt, String timeZone, String authenticationMethod, String status) {
        this.id = id;
        this.loginAt = loginAt;
        this.timeZone = timeZone != null ? timeZone : "UTC";
        this.authenticationMethod = authenticationMethod;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Instant loginAt) {
        this.loginAt = loginAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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
        private Instant loginAt;
        private String timeZone = "UTC";
        private String authenticationMethod;
        private String status = "SUCCESS";

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder loginAt(Instant loginAt) {
            this.loginAt = loginAt;
            return this;
        }

        public Builder timeZone(String timeZone) {
            this.timeZone = timeZone;
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

        public LoginHistoryResponse build() {
            return new LoginHistoryResponse(id, loginAt, timeZone, authenticationMethod, status);
        }
    }
}

