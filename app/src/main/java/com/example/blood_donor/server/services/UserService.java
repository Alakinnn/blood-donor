package com.example.blood_donor.server.services;

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
import com.example.blood_donor.ui.manager.AuthManager;

import java.util.Optional;
import java.util.UUID;

public class UserService implements IUserService {
    private final IUserRepository userRepository;
    private final ISessionRepository sessionRepository;
    private final IAuthService authService;

    public UserService(
            IUserRepository userRepository,
            ISessionRepository sessionRepository,
            IAuthService authService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.authService = authService;
    }

    @Override
    public ApiResponse<AuthResponse> registerDonor(DonorRegisterRequest request) {
        try {
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
            User savedUser = userRepository.createUser(newUser).orElseThrow(() -> new AppException(ErrorCode.DATABASE_ERROR, "Failed to create user"));;

            String token = authService.generateToken(savedUser);
            sessionRepository.saveSession(token, savedUser.getUserId());

            AuthManager.getInstance().saveAuthToken(
                    token,
                    savedUser.getUserId(),
                    savedUser.getUserType(),
                    savedUser.getFullName()
            );

            return ApiResponse.success(new AuthResponse(savedUser, token));

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<AuthResponse> registerManager(ManagerRegisterRequest request) {
        try {
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

            // Add this
            AuthManager.getInstance().saveAuthToken(
                    token,
                    savedUser.getUserId(),
                    savedUser.getUserType(),
                    savedUser.getFullName()
            );

            return ApiResponse.success(new AuthResponse(savedUser, token));

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }
    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        try {
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

            // Save to AuthManager - Add this
            AuthManager.getInstance().saveAuthToken(
                    sessionToken,
                    user.getUserId(),
                    user.getUserType(),
                    user.getFullName()
            );

            return ApiResponse.success(new AuthResponse(user, sessionToken));

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ApiResponse<Void> logout(String token) {
        try {
            sessionRepository.deleteSession(token);
            return ApiResponse.success(null);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ApiResponse<Void> logoutAll(String userId) {
        try {
            sessionRepository.deleteAllUserSessions(userId);
            return ApiResponse.success(null);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
