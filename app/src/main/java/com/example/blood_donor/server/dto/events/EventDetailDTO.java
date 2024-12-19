package com.example.blood_donor.server.dto.events;

import com.example.blood_donor.server.models.event.EventStatus;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class EventDetailDTO {
    private String eventId;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private EventStatus status;

    // Host info
    private String hostId;
    private String hostName;
    private String hostPhoneNumber;

    // Location
    private String address;
    private double latitude;
    private double longitude;
    private String locationDescription;

    // Blood info
    private List<String> requiredBloodTypes;
    private double bloodGoal;
    private double currentBloodCollected;

    // Registration counts
    private int donorCount;
    private int volunteerCount;
    private List<BloodTypeProgress> bloodProgress;
    private Map<String, Integer> donorsByBloodType;
    private LocalTime donationStartTime;
    private LocalTime donationEndTime;
    public EventDetailDTO(String eventId, String title, String description,
                          long startTime, long endTime, EventStatus status,
                          String hostId, String hostName, String hostPhoneNumber,
                          String address, double latitude, double longitude,
                          String locationDescription, List<String> requiredBloodTypes,
                          double bloodGoal, double currentBloodCollected,
                          int donorCount, int volunteerCount,
                          List<BloodTypeProgress> bloodProgress,  // Add this parameter
                          LocalTime donationStartTime,            // And these two
                          LocalTime donationEndTime) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.hostId = hostId;
        this.hostName = hostName;
        this.hostPhoneNumber = hostPhoneNumber;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
        this.requiredBloodTypes = requiredBloodTypes;
        this.bloodGoal = bloodGoal;
        this.currentBloodCollected = currentBloodCollected;
        this.donorCount = donorCount;
        this.volunteerCount = volunteerCount;
        this.bloodProgress = bloodProgress;
        this.donationStartTime = donationStartTime;
        this.donationEndTime = donationEndTime;
    }

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

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostPhoneNumber() {
        return hostPhoneNumber;
    }

    public void setHostPhoneNumber(String hostPhoneNumber) {
        this.hostPhoneNumber = hostPhoneNumber;
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

    public int getDonorCount() {
        return donorCount;
    }

    public void setDonorCount(int donorCount) {
        this.donorCount = donorCount;
    }

    public int getVolunteerCount() {
        return volunteerCount;
    }

    public void setVolunteerCount(int volunteerCount) {
        this.volunteerCount = volunteerCount;
    }
    // Add getters and setters
    public LocalTime getDonationStartTime() { return donationStartTime; }
    public void setDonationStartTime(LocalTime donationStartTime) {
        this.donationStartTime = donationStartTime;
    }
    public LocalTime getDonationEndTime() { return donationEndTime; }
    public void setDonationEndTime(LocalTime donationEndTime) {
        this.donationEndTime = donationEndTime;
    }
}