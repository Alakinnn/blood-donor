package com.example.blood_donor.server.dto.events;

import java.util.List;

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
    private double latitude;
    private double longitude;

    // Constructor
    public EventMarkerDTO(String eventId, String title, String address, long startTime,
                          long endTime, List<String> requiredBloodTypes, double bloodGoal,
                          double currentBloodCollected, int registeredDonors,
                          double latitude, double longitude) {
        this.eventId = eventId;
        this.title = title;
        this.address = address;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredBloodTypes = requiredBloodTypes;
        this.bloodGoal = bloodGoal;
        this.currentBloodCollected = currentBloodCollected;
        this.registeredDonors = registeredDonors;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public List<String> getRequiredBloodTypes() { return requiredBloodTypes; }
    public void setRequiredBloodTypes(List<String> requiredBloodTypes) { this.requiredBloodTypes = requiredBloodTypes; }

    public double getBloodGoal() { return bloodGoal; }
    public void setBloodGoal(double bloodGoal) { this.bloodGoal = bloodGoal; }

    public double getCurrentBloodCollected() { return currentBloodCollected; }
    public void setCurrentBloodCollected(double currentBloodCollected) { this.currentBloodCollected = currentBloodCollected; }

    public int getRegisteredDonors() { return registeredDonors; }
    public void setRegisteredDonors(int registeredDonors) { this.registeredDonors = registeredDonors; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}