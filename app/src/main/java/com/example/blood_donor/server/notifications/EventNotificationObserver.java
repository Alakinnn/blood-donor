package com.example.blood_donor.server.notifications;

import android.content.Context;

public class EventNotificationObserver implements NotificationObserver {
    private final Context context;
    private final String userId;

    public EventNotificationObserver(Context context, String userId) {
        this.context = context;
        this.userId = userId;
    }

    @Override
    public void onEventCreated(String eventId, String eventTitle) {
        NotificationTemplate notification = new NotificationTemplate.Builder()
                .title("New Blood Donation Event")
                .content(String.format("New event created: %s", eventTitle))
                .channelId(NotificationConstants.EVENT_CHANNEL_ID)
                .build();

        NotificationSender.send(context, notification, generateNotificationId("create", eventId));
    }

    @Override
    public void onParticipantJoined(String eventId, String participantName) {
        NotificationTemplate notification = new NotificationTemplate.Builder()
                .title("New Participant")
                .content(String.format("%s has joined your event", participantName))
                .channelId(NotificationConstants.EVENT_CHANNEL_ID)
                .build();

        NotificationSender.send(context, notification, generateNotificationId("join", eventId));
    }

    @Override
    public void onEventUpdated(String eventId, String updateDetails) {
        NotificationTemplate notification = new NotificationTemplate.Builder()
                .title("Event Updated")
                .content(updateDetails)
                .channelId(NotificationConstants.EVENT_CHANNEL_ID)
                .build();

        NotificationSender.send(context, notification, generateNotificationId("update", eventId));
    }

    private int generateNotificationId(String type, String eventId) {
        return (type + eventId).hashCode();
    }
}