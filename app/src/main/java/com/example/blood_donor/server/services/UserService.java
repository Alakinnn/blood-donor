package com.example.blood_donor.server.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.dto.auth.AuthResponse;
import com.example.blood_donor.server.dto.auth.DonorRegisterRequest;
import com.example.blood_donor.server.dto.auth.LoginRequest;
import com.example.blood_donor.server.dto.auth.ManagerRegisterRequest;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.repositories.ISessionRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.services.interfaces.IAuthService;
import com.example.blood_donor.server.services.interfaces.IUserService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserService implements IUserService {
    private static final String TAG = "UserService";
    private final DatabaseHelper dbHelper;
    private final IUserRepository userRepository;
    private final ISessionRepository sessionRepository;
    private final IAuthService authService;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public UserService(
            DatabaseHelper dbHelper,
            IUserRepository userRepository,
            ISessionRepository sessionRepository,
            IAuthService authService
    ) {
        this.dbHelper = dbHelper;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.authService = authService;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public ApiResponse<AuthResponse> registerDonor(DonorRegisterRequest request) {
        try {
            return dbHelper.executeTransaction(db -> {
                // Validate request
                request.validate();

                // Check if email exists
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }

                // Hash password
                String hashedPassword = authService.hashPassword(request.getPassword());

                // Create user
                User newUser = new User(
                        UUID.randomUUID().toString(),
                        request.getEmail(),
                        hashedPassword,
                        request.getFullName(),
                        request.getDateOfBirth(),
                        null,  // No phone number for donors
                        UserType.DONOR,
                        request.getBloodType(),
                        request.getGender()
                );

                // Save user
                User savedUser = userRepository.createUser(newUser)
                        .orElseThrow(() -> new AppException(ErrorCode.DATABASE_ERROR, "Failed to create user"));

                // Generate and save session token
                String token = authService.generateToken(savedUser);
                sessionRepository.saveSession(token, savedUser.getUserId());

                return ApiResponse.success(new AuthResponse(savedUser, token));
            });
        } catch (AppException e) {
            Log.e(TAG, "Error registering donor", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<AuthResponse> registerManager(ManagerRegisterRequest request) {
        try {
            return dbHelper.executeTransaction(db -> {
                request.validate();

                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }

                String hashedPassword = authService.hashPassword(request.getPassword());
                User newUser = new User(
                        UUID.randomUUID().toString(),
                        request.getEmail(),
                        hashedPassword,
                        request.getFullName(),
                        request.getDateOfBirth(),
                        request.getPhoneNumber(),
                        UserType.SITE_MANAGER,
                        null,
                        request.getGender()
                );

                User savedUser = userRepository.createUser(newUser)
                        .orElseThrow(() -> new AppException(ErrorCode.DATABASE_ERROR, "Failed to create user"));

                String token = authService.generateToken(savedUser);
                sessionRepository.saveSession(token, savedUser.getUserId());

                return ApiResponse.success(new AuthResponse(savedUser, token));
            });
        } catch (AppException e) {
            Log.e(TAG, "Error registering manager", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        try {
            return dbHelper.executeTransaction(db -> {
                // Get user by email
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

                // Verify password
                if (!authService.verifyPassword(request.getPassword(), user.getPassword())) {
                    throw new AppException(ErrorCode.INVALID_CREDENTIALS);
                }

                // Generate session token
                String sessionToken = authService.generateToken(user);

                // Save session
                sessionRepository.saveSession(sessionToken, user.getUserId());

                return ApiResponse.success(new AuthResponse(user, sessionToken));
            });
        } catch (AppException e) {
            Log.e(TAG, "Login failed", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during login", e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ApiResponse<Void> logout(String token) {
        try {
            return dbHelper.executeTransaction(db -> {
                sessionRepository.deleteSession(token);
                return ApiResponse.success(null);
            });
        } catch (AppException e) {
            Log.e(TAG, "Logout failed", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<Void> logoutAll(String userId) {
        try {
            return dbHelper.executeTransaction(db -> {
                sessionRepository.deleteAllUserSessions(userId);
                return ApiResponse.success(null);
            });
        } catch (AppException e) {
            Log.e(TAG, "LogoutAll failed", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }
}