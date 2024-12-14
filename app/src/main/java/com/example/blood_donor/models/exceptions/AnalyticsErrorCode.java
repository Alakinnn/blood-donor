package com.example.blood_donor.models.exceptions;

public enum AnalyticsErrorCode {
    GENERAL_ERROR("An error occurred during analytics processing"),
    INVALID_FORMAT("Invalid or unsupported report format"),
    INVALID_TIMEFRAME("Invalid report timeframe"),
    INVALID_USER("User not found or invalid permissions"),
    NO_DATA("No data available for the specified criteria"),
    PROCESSING_ERROR("Error processing analytics data"),
    EXPORT_ERROR("Error exporting report"),
    FILE_IO_ERROR("Error reading or writing report file");

    private final String message;

    AnalyticsErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
