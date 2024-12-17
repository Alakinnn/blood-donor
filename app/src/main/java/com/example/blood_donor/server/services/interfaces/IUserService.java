package com.example.blood_donor.server.services.interfaces;

import com.example.blood_donor.server.dto.auth.AuthResponse;
import com.example.blood_donor.server.dto.auth.DonorRegisterRequest;
import com.example.blood_donor.server.dto.auth.LoginRequest;
import com.example.blood_donor.server.dto.auth.ManagerRegisterRequest;
import com.example.blood_donor.server.models.response.ApiResponse;

public interface IUserService {
    ApiResponse<AuthResponse> registerDonor(DonorRegisterRequest request);

    ApiResponse<AuthResponse> registerManager(ManagerRegisterRequest request);

    ApiResponse<AuthResponse> login(LoginRequest request);
    ApiResponse<Void> logout(String token);
    ApiResponse<Void> logoutAll(String userId);

    void shutdown();
}
