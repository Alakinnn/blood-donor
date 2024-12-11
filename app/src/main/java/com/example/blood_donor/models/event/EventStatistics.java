package com.example.blood_donor.models.event;

import java.util.HashMap;
import java.util.Map;

public class EventStatistics {
    private String eventId;
    private int totalDonors;
    private int totalVolunteers;
    private Map<String, Double> bloodTypeCollected;
    private long generatedAt;

    // Constructor
    public EventStatistics(String eventId) {
        this.eventId = eventId;
        this.totalDonors = 0;
        this.totalVolunteers = 0;
        this.bloodTypeCollected = new HashMap<>();
        this.generatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    // ... (all standard getters and setters)
}

