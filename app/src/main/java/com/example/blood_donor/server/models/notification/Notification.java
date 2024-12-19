package com.example.blood_donor.server.models.notification;

public class Notification {
    private String notificationId;
    private String title;
    private String message;
    private long timestamp;
    private NotificationType type;
    private String recipientId;
    private boolean isRead;
    private String eventId;

    // Constructor


    public Notification(String notificationId, String title, String message, long timestamp, NotificationType type, String recipientId, boolean isRead, String eventId) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.recipientId = recipientId;
        this.isRead = isRead;
        this.eventId = eventId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }


    // Getters and setters
    // ... (all standard getters and setters)
}
