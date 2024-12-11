package com.example.blood_donor.dto.auth;

import com.example.blood_donor.models.user.User;

public class AuthResponse {
    private User user;
    private String token;

    public AuthResponse(User user, String token) {
        this.user = user;
        this.token = token;
    }

    // Getters

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
