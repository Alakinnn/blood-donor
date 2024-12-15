package com.example.blood_donor.server.notifications;

import static com.example.blood_donor.server.notifications.NotificationConstants.EVENT_CHANNEL_ID;
import static com.example.blood_donor.server.notifications.NotificationConstants.EVENT_CHANNEL_NAME;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventNotificationHandler {
    private final NotificationManager notificationManager;
    private final Context context;
    private final ScheduledExecutorService scheduler;

    public EventNotificationHandler(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.scheduler = Executors.newScheduledThreadPool(1);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    EVENT_CHANNEL_ID,
                    EVENT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Schedule event reminders
    public void scheduleEventReminders(String eventId, String eventTitle, long eventTime) {
        // 24 hours before
        scheduler.schedule(
                () -> sendEventReminder(eventId, eventTitle, "24 hours"),
                eventTime - System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24),
                TimeUnit.MILLISECONDS
        );

        // 1 hour before
        scheduler.schedule(
                () -> sendEventReminder(eventId, eventTitle, "1 hour"),
                eventTime - System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                TimeUnit.MILLISECONDS
        );
    }

    private void sendEventReminder(String eventId, String eventTitle, String timeframe) {
        NotificationTemplate notification = new NotificationTemplate.Builder()
                .title("Upcoming Event Reminder")
                .content(String.format("%s starts in %s", eventTitle, timeframe))
                .channelId(EVENT_CHANNEL_ID)
                .priority(NotificationManager.IMPORTANCE_HIGH)
                .build();

        // Send to NotificationSender
        NotificationSender.send(context, notification, generateNotificationId(eventId));
    }

    private int generateNotificationId(String eventId) {
        return eventId.hashCode();
    }
}
