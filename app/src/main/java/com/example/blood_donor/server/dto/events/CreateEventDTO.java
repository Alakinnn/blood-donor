package com.example.blood_donor.server.dto.events;

import java.util.List;
import java.util.Map;

public class CreateEventDTO {
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private double bloodGoal;
    // Location details
    private String address;
    private double latitude;
    private double longitude;
    private String locationDescription;
    private Map<String, Double> bloodTypeTargets;

    // Constructor, getters, setters
    public CreateEventDTO(String title, String description, long startTime,
                          long endTime, double bloodGoal,
                          String address, double latitude, double longitude,
                          String locationDescription) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bloodGoal = bloodGoal;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
    }

    // Add getters and setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public Map<String, Double> getBloodTypeTargets() {
        return bloodTypeTargets;
    }

    public void setBloodTypeTargets(Map<String, Double> bloodTypeTargets) {
        this.bloodTypeTargets = bloodTypeTargets;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }
}
