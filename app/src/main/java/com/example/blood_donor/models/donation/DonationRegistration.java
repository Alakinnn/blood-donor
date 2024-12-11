package com.example.blood_donor.models.donation;

public class DonationRegistration {
    private String registrationId;
    private String donorId;
    private String eventId;
    private long registrationTime;

    // Constructor
    public DonationRegistration(String registrationId, String donorId,
                                String eventId, long registrationTime) {
        this.registrationId = registrationId;
        this.donorId = donorId;
        this.eventId = eventId;
        this.registrationTime = registrationTime;
    }

    // Getters and setters
    // ... (all standard getters and setters)
}