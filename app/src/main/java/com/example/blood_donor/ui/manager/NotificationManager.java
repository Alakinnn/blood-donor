package com.example.blood_donor.ui.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.notifications.NotificationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NotificationManager {
    private static final String NOTIFICATION_TABLE = "notifications";
    private final DatabaseHelper dbHelper;
    private final Context context;
    private NotificationCallback notificationCallback;


    private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long TWENTY_FOUR_HOURS = TimeUnit.HOURS.toMillis(24);
    public NotificationManager(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void createEventNotification(String userId, String eventId, String title, String message, NotificationType type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("notification_id", UUID.randomUUID().toString());
        values.put("user_id", userId);
        values.put("event_id", eventId);
        values.put("title", title);
        values.put("message", message);
        values.put("type", type.name());
        values.put("is_read", 0);
        values.put("created_at", System.currentTimeMillis());

        db.insert(NOTIFICATION_TABLE, null, values);
    }

    public List<NotificationItem> getUnreadNotifications(String userId) {
        List<NotificationItem> notifications = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                NOTIFICATION_TABLE,
                null,
                "user_id = ? AND is_read = 0",
                new String[]{userId},
                null, null, "created_at DESC"
        );

        while (cursor.moveToNext()) {
            notifications.add(cursorToNotification(cursor));
        }
        cursor.close();

        return notifications;
    }

    public List<NotificationItem> getAllNotifications(String userId) {
        List<NotificationItem> notifications = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                NOTIFICATION_TABLE,
                null,
                "user_id = ?",
                new String[]{userId},
                null, null, "created_at DESC"
        );

        while (cursor.moveToNext()) {
            notifications.add(cursorToNotification(cursor));
        }
        cursor.close();

        return notifications;
    }

    public void markAsRead(String notificationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);

        db.update(NOTIFICATION_TABLE,
                values,
                "notification_id = ?",
                new String[]{notificationId});
    }

    public void markAllAsRead(String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);

        db.update(NOTIFICATION_TABLE,
                values,
                "user_id = ? AND is_read = 0",
                new String[]{userId});
    }

    public void deleteNotification(String notificationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(NOTIFICATION_TABLE,
                "notification_id = ?",
                new String[]{notificationId});
    }

    public void deleteAllNotifications(String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(NOTIFICATION_TABLE,
                "user_id = ?",
                new String[]{userId});

        // Ensure notification dot is updated
        notifyNotificationCountChanged(userId);
    }

    private void notifyNotificationCountChanged(String userId) {
        // This will be called after any operation that changes the notification count
        List<NotificationItem> unread = getUnreadNotifications(userId);
        boolean hasUnread = !unread.isEmpty();

        // You can implement a callback or event system here to update the UI
        if (notificationCallback != null) {
            notificationCallback.onNotificationCountChanged(hasUnread);
        }
    }

    public interface NotificationCallback {
        void onNotificationCountChanged(boolean hasUnread);
    }

    public void setNotificationCallback(NotificationCallback callback) {
        this.notificationCallback = callback;
    }

    private NotificationItem cursorToNotification(Cursor cursor) {
        return new NotificationItem(
                cursor.getString(cursor.getColumnIndexOrThrow("notification_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("message")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1
        );
    }

    public void checkBloodTypeMatchingEvents(String userId, String userBloodType) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query =
                "SELECT e.id AS event_id, e.title, e.blood_type_targets " +
                        "FROM events e " +
                        "WHERE e.blood_type_targets LIKE ? " +
                        "AND e.status = 'UPCOMING' " +
                        "AND NOT EXISTS (SELECT 1 FROM notifications n " +
                        "               WHERE n.user_id = ? " +
                        "               AND n.event_id = e.id " +
                        "               AND n.type = 'BLOOD_TYPE_MATCH')";

        try (Cursor cursor = db.rawQuery(query, new String[]{"%" + userBloodType + "%", userId})) {
            while (cursor.moveToNext()) {
                String eventId = cursor.getString(cursor.getColumnIndexOrThrow("event_id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));

                createEventNotification(
                        userId,
                        eventId,
                        "Blood Type Match Found",
                        String.format("New event matching your blood type (%s): %s", userBloodType, title),
                        NotificationType.BLOOD_TYPE_MATCH
                );
            }
        }
    }

    public void scheduleEventReminders(String userId, String eventId, String eventTitle, long eventTime) {
        long currentTime = System.currentTimeMillis();
        long oneHourBefore = eventTime - ONE_HOUR;
        long dayBefore = eventTime - TWENTY_FOUR_HOURS;

        // Schedule 24-hour reminder
        if (currentTime < dayBefore) {
            createEventNotification(
                    userId,
                    eventId,
                    "Upcoming Event Reminder",
                    String.format("%s starts in 24 hours", eventTitle),
                    NotificationType.REMINDER
            );
        }

        // Schedule 1-hour reminder
        if (currentTime < oneHourBefore) {
            createEventNotification(
                    userId,
                    eventId,
                    "Event Starting Soon",
                    String.format("%s starts in 1 hour", eventTitle),
                    NotificationType.REMINDER
            );
        }
    }

    public void notifyEventUpdate(String eventId, String eventTitle, List<String> affectedUserIds) {
        for (String userId : affectedUserIds) {
            createEventNotification(
                    userId,
                    eventId,
                    "Event Updated",
                    String.format("Changes have been made to: %s", eventTitle),
                    NotificationType.EVENT_UPDATE
            );
        }
    }
}

