package com.example.blood_donor.server.validators;

import com.example.blood_donor.server.dto.auth.RegisterRequest;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterRequestValidator implements IValidator<RegisterRequest> {
    @Override
    public void validate(RegisterRequest request) throws AppException {
        List<String> errors = new ArrayList<>();

        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Full name is required");
        }

        if (request.getBloodType() == null || !isValidBloodType(request.getBloodType())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid blood type");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidBloodType(String bloodType) {
        return Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                .contains(bloodType);
    }
}
