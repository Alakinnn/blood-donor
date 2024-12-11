package com.example.blood_donor.services.interfaces;

import com.example.blood_donor.dto.auth.AuthResponse;
import com.example.blood_donor.dto.auth.LoginRequest;
import com.example.blood_donor.dto.auth.RegisterRequest;
import com.example.blood_donor.models.response.ApiResponse;

public interface IUserService {
    ApiResponse<AuthResponse> register(RegisterRequest request);
    ApiResponse<AuthResponse> login(LoginRequest request);
}
