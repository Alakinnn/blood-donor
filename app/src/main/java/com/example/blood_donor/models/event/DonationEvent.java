package com.example.blood_donor.models.event;

import com.example.blood_donor.models.location.Location;

import java.util.List;

public class DonationEvent {
    private String eventId;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private Location location;
    private List<String> requiredBloodTypes;
    private double bloodGoal;
    private double currentBloodCollected;
    private String hostId;
    private EventStatus status;

    // Constructor
    public DonationEvent(String eventId, String title, String description,
                         long startTime, long endTime, Location location,
                         List<String> requiredBloodTypes, double bloodGoal,
                         String hostId) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.requiredBloodTypes = requiredBloodTypes;
        this.bloodGoal = bloodGoal;
        this.currentBloodCollected = 0.0;
        this.hostId = hostId;
        this.status = EventStatus.UPCOMING;
    }

    // Getters and setters
    // ... (all standard getters and setters)
}