package com.example.blood_donor.server.models.donation;

public class Registration {
    private String registrationId;
    private String userId;
    private String eventId;
    private RegistrationType type;
    private long registrationTime;

    public Registration(String registrationId, String userId, String eventId,
                        RegistrationType type) {
        this.registrationId = registrationId;
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.registrationTime = System.currentTimeMillis();
    }

    // Getters only since we don't need to modify after creation
    public String getRegistrationId() { return registrationId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public RegistrationType getType() { return type; }
    public long getRegistrationTime() { return registrationTime; }
}