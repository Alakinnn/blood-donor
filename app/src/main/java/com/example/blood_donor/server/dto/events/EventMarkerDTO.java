package com.example.blood_donor.server.dto.events;

import java.util.List;

// For map marker clicks
public class EventMarkerDTO {
    private String eventId;
    private String title;
    private String address;
    private long startTime;
    private long endTime;
    private List<String> requiredBloodTypes;
    private double bloodGoal;
    private double currentBloodCollected;
    private int registeredDonors;

    // Constructor, getters, setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<String> getRequiredBloodTypes() {
        return requiredBloodTypes;
    }

    public void setRequiredBloodTypes(List<String> requiredBloodTypes) {
        this.requiredBloodTypes = requiredBloodTypes;
    }

    public double getBloodGoal() {
        return bloodGoal;
    }

    public void setBloodGoal(double bloodGoal) {
        this.bloodGoal = bloodGoal;
    }

    public double getCurrentBloodCollected() {
        return currentBloodCollected;
    }

    public void setCurrentBloodCollected(double currentBloodCollected) {
        this.currentBloodCollected = currentBloodCollected;
    }

    public int getRegisteredDonors() {
        return registeredDonors;
    }

    public void setRegisteredDonors(int registeredDonors) {
        this.registeredDonors = registeredDonors;
    }
}
