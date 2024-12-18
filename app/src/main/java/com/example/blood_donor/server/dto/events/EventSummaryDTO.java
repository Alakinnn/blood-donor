package com.example.blood_donor.server.dto.events;

import java.time.LocalTime;
import java.util.List;

public class EventSummaryDTO {
    private String eventId;
    private String title;
    private double latitude;
    private double longitude;
    private List<String> requiredBloodTypes;
    private LocalTime donationStartTime;
    private LocalTime donationEndTime;
    private long startTime;
    private long endTime;
    private double bloodGoal;
    private double currentBloodCollected;
    private double distance;
    private List<BloodTypeProgress> bloodProgress;
    private double totalProgress;private String address;
    private int registeredDonors;
    private int registeredVolunteers;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getRegisteredVolunteers() {
        return registeredVolunteers;
    }

    public void setRegisteredVolunteers(int registeredVolunteers) {
        this.registeredVolunteers = registeredVolunteers;
    }


    public int getRegisteredDonors() { return registeredDonors; }
    public void setRegisteredDonors(int registeredDonors) { this.registeredDonors = registeredDonors; }

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
    public LocalTime getDonationStartTime() { return donationStartTime; }
    public void setDonationStartTime(LocalTime donationStartTime) {
        this.donationStartTime = donationStartTime;
    }
    public LocalTime getDonationEndTime() { return donationEndTime; }
    public void setDonationEndTime(LocalTime donationEndTime) {
        this.donationEndTime = donationEndTime;
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

    public List<BloodTypeProgress> getBloodProgress() {
        return bloodProgress;
    }

    public void setBloodProgress(List<BloodTypeProgress> bloodProgress) {
        this.bloodProgress = bloodProgress;
    }

    public double getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(double totalProgress) {
        this.totalProgress = totalProgress;
    }
}
