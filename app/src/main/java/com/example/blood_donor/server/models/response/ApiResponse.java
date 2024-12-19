package com.example.blood_donor.server.models.response;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;

public class ApiResponse<T> {
    private T data;
    private boolean success;
    private ErrorCode errorCode;
    private String message;

    private ApiResponse(T data, boolean success, ErrorCode errorCode, String message) {
        this.data = data;
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, true, null, "Success");
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(null, false, errorCode, errorCode.getMessage());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(null, false, errorCode, message);
    }

    // Getters
    public T getData() { return data; }
    public boolean isSuccess() { return success; }
    public ErrorCode getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public void throwErrorIfPresent() throws AppException {
        if (!success && errorCode != null) {
            throw new AppException(errorCode, message);
        }
    }
}
