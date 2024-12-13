package com.example.blood_donor.dto.auth;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;

import java.util.regex.Pattern;

public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private long dateOfBirth;
    private String bloodType;
    private String gender;

    // Constructor, getters, setters

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(long dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void validate() throws AppException {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (password == null || password.length() < 8) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Full name is required");
        }

        if (dateOfBirth <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid date of birth");
        }

        if (gender == null || gender.trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Gender is required");
        }
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );
}


