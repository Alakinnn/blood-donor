package com.example.blood_donor.errors;

public enum ErrorCode {
    // Auth errors
    INVALID_CREDENTIALS("Invalid email or password", 1001),
    EMAIL_ALREADY_EXISTS("Email already registered", 1002),
    INVALID_EMAIL_FORMAT("Invalid email format", 1003),
    INVALID_PASSWORD_FORMAT("Password must be at least 8 characters", 1004),
    INVALID_TOKEN("Invalid or expired token", 1005),

    // Database errors
    DATABASE_ERROR("Database operation failed", 2001),

    // Validation errors
    INVALID_INPUT("Invalid input data", 3001),
    MISSING_REQUIRED_FIELD("Required field is missing", 3002),

    // Server errors
    INTERNAL_SERVER_ERROR("Internal server error", 5001);

    private final String message;
    private final int code;

    ErrorCode(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() { return message; }
    public int getCode() { return code; }
}
