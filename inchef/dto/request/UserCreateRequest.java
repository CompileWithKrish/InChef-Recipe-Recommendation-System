package com.inchef.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    private String avatarUrl;

    public UserCreateRequest() {}

    public UserCreateRequest(String email, String name, String phone, String avatarUrl) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String email;
        private String name;
        private String phone;
        private String avatarUrl;

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

        public UserCreateRequest build() {
            UserCreateRequest obj = new UserCreateRequest();
            obj.setEmail(this.email);
            obj.setName(this.name);
            obj.setPhone(this.phone);
            obj.setAvatarUrl(this.avatarUrl);
            return obj;
        }
    }
}
