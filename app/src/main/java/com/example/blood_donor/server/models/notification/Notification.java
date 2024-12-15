package com.example.blood_donor.server.models.notification;

public class Notification {
    private String notificationId;
    private String message;
    private long timestamp;
    private NotificationType type;
    private String recipientId;
    private boolean isRead;

    // Constructor
    public Notification(String notificationId, String message,
                        NotificationType type, String recipientId) {
        this.notificationId = notificationId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.recipientId = recipientId;
        this.isRead = false;
    }

    // Getters and setters
    // ... (all standard getters and setters)
}
