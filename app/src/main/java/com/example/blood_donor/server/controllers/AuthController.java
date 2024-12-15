package com.example.blood_donor.server.controllers;

import com.example.blood_donor.server.dto.auth.AuthResponse;
import com.example.blood_donor.server.dto.auth.LoginRequest;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.interfaces.IUserService;

public class AuthController {
    private final IUserService userService;

    public AuthController(IUserService userService) {
        this.userService = userService;
    }

    public ApiResponse<AuthResponse> login(LoginRequest request) {
        return userService.login(request);
    }

    public ApiResponse<Void> logout(String token) {
        return userService.logout(token);
    }

    public ApiResponse<Void> logoutAll(String userId) {
        return userService.logoutAll(userId);
    }
}
