package com.example.blood_donor.models.exceptions;

public class AnalyticsException extends Exception {
    private final AnalyticsErrorCode errorCode;

    public AnalyticsException(String message) {
        super(message);
        this.errorCode = AnalyticsErrorCode.GENERAL_ERROR;
    }

    public AnalyticsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = AnalyticsErrorCode.GENERAL_ERROR;
    }

    public AnalyticsException(AnalyticsErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AnalyticsException(AnalyticsErrorCode errorCode, String additionalInfo) {
        super(errorCode.getMessage() + ": " + additionalInfo);
        this.errorCode = errorCode;
    }

    public AnalyticsErrorCode getErrorCode() {
        return errorCode;
    }
}
