package com.example.blood_donor.dto.auth;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;

import java.util.Arrays;

public class DonorRegisterRequest extends RegisterRequest {
    public void validate() throws AppException {
        // Validate base request fields
        super.validate();

        // Additional donor-specific validation for blood type
        if (getBloodType() == null ||
                !Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                        .contains(getBloodType())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid blood type for donor");
        }
    }
}