package com.example.blood_donor.server.dto.auth;

import com.example.blood_donor.server.models.user.User;

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
