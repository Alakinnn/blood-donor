package com.example.blood_donor.server.notifications;

public interface NotificationObserver {
    void onEventCreated(String eventId, String eventTitle);
    void onParticipantJoined(String eventId, String participantName);
    void onEventUpdated(String eventId, String updateDetails);
}
