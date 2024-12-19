package com.example.blood_donor.server.repositories;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.models.notification.Notification;

import java.util.List;

public class NotificationRepository {
    private final DatabaseHelper dbHelper;
    private static final String TABLE_NOTIFICATIONS = "notifications";

    public NotificationRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void saveNotification(Notification notification) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("notification_id", notification.getNotificationId());
        values.put("title", notification.getTitle());
        values.put("message", notification.getMessage());
        values.put("timestamp", notification.getTimestamp());
        values.put("type", notification.getType().name());
        values.put("recipient_id", notification.getRecipientId());
        values.put("is_read", notification.isRead() ? 1 : 0);
        values.put("event_id", notification.getEventId());

        db.insert(TABLE_NOTIFICATIONS, null, values);
    }

    public List<Notification> getUnreadNotifications(String userId) {
        // Implementation to fetch unread notifications
        return java.util.Collections.emptyList();
    }

    public void markAsRead(String notificationId) {
        // Implementation to mark notification as read
    }

    public void deleteNotification(String notificationId) {
        // Implementation to delete notification
    }
}
