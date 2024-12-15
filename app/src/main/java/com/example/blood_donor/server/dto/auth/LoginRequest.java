package com.example.blood_donor.server.dto.auth;

public class LoginRequest {
    private String email;
    private String password;

    // Constructor, getters, setters


    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
