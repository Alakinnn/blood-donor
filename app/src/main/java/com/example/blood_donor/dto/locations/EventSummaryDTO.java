package com.example.blood_donor.dto.locations;

import java.util.List;

public class EventSummaryDTO {
    private String eventId;
    private String title;
    private double latitude;
    private double longitude;
    private List<String> requiredBloodTypes;
    private long startTime;
    private long endTime;
    private double bloodGoal;
    private double currentBloodCollected;
    private double distance;  // Optional, set only when location provided in query

    // Constructor, getters, setters...

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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public List<String> getRequiredBloodTypes() {
        return requiredBloodTypes;
    }

    public void setRequiredBloodTypes(List<String> requiredBloodTypes) {
        this.requiredBloodTypes = requiredBloodTypes;
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
