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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(int totalDonors) {
        this.totalDonors = totalDonors;
    }

    public int getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(int totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public Map<String, Double> getBloodTypeCollected() {
        return bloodTypeCollected;
    }

    public void setBloodTypeCollected(Map<String, Double> bloodTypeCollected) {
        this.bloodTypeCollected = bloodTypeCollected;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }
}

