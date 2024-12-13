package com.example.blood_donor.dto.auth;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;

public class ManagerRegisterRequest extends RegisterRequest {
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void validate() throws AppException {
        // Validate base request fields
        super.validate();

        // Remove blood type validation
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Phone number is required for site manager");
        }

        // Phone number format validation (basic regex)
        if (!phoneNumber.matches("^\\+?\\d{10,14}$")) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid phone number format");
        }
    }
}