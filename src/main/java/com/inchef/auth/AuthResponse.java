package com.inchef.auth;

public class AuthResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private String authProvider;
    private Boolean emailVerified;
    private String message;

    public AuthResponse() {}

    public AuthResponse(Long id, String email, String name, String phone, String avatarUrl, String authProvider, Boolean emailVerified, String message) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.authProvider = authProvider;
        this.emailVerified = emailVerified;
        this.message = message;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }
    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
