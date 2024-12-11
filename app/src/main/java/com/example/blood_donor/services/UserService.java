package com.example.blood_donor.services;

import com.example.blood_donor.dto.auth.AuthResponse;
import com.example.blood_donor.dto.auth.LoginRequest;
import com.example.blood_donor.dto.auth.RegisterRequest;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.interfaces.IAuthService;
import com.example.blood_donor.services.interfaces.IUserService;
import com.example.blood_donor.validators.IValidator;

import java.util.Optional;
import java.util.UUID;

public class UserService implements IUserService {
    private final IUserRepository userRepository;
    private final IAuthService authService;
    private final IValidator<RegisterRequest> registerValidator;

    public UserService(
            IUserRepository userRepository,
            IAuthService authService,
            IValidator<RegisterRequest> registerValidator
    ) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.registerValidator = registerValidator;
    }

    @Override
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        try {
            // Validate request
            registerValidator.validate(request);

            // Check if email exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            // Create user
            String hashedPassword = authService.hashPassword(request.getPassword());
            User newUser = new User(
                    UUID.randomUUID().toString(),
                    request.getEmail(),
                    hashedPassword,
                    request.getFullName(),
                    request.getDateOfBirth(),
                    null,
                    UserType.DONOR,
                    request.getBloodType(),
                    request.getGender()
            );

            // Save user and handle Optional
            Optional<User> savedUserOptional = userRepository.createUser(newUser);
            User savedUser = savedUserOptional.orElseThrow(
                    () -> new AppException(ErrorCode.DATABASE_ERROR, "Failed to create user")
            );

            // Generate token
            String token = authService.generateToken(savedUser);

            return ApiResponse.success(new AuthResponse(savedUser, token));

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
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

            // Generate token
            String token = authService.generateToken(user);

            return ApiResponse.success(new AuthResponse(user, token));

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
