package com.example.btland.models;

import com.google.firebase.firestore.PropertyName;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    @PropertyName("isAdmin")
    private boolean isAdmin;
    @PropertyName("isBanned")
    private boolean isBanned;

    public User() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @PropertyName("isAdmin")
    public boolean isAdmin() {
        return isAdmin;
    }

    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @PropertyName("isBanned")
    public boolean isBanned() {
        return isBanned;
    }

    @PropertyName("isBanned")
    public void setBanned(boolean banned) {
        isBanned = banned;
    }
}
