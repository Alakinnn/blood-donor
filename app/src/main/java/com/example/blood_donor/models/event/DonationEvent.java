package com.example.blood_donor.models.event;

import com.example.blood_donor.models.location.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonationEvent {
    private String eventId;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private Location location;
    private double currentBloodCollected;
    private String hostId;
    private EventStatus status;
    private Double distance; // Add this field
    // Getters and setters
    private int donorCount;
    private int volunteerCount;
    private String hostName;
    private String hostPhoneNumber;
    private Map<String, BloodTypeRequirement> bloodRequirements;

    public DonationEvent(String eventId, String title, String description,
                         long startTime, long endTime, Location location,
                         Map<String, Double> bloodTypeTargets, String hostId) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.currentBloodCollected = 0.0;
        this.hostId = hostId;
        this.status = EventStatus.UPCOMING;
        this.distance = null;
        this.donorCount = 0;
        this.volunteerCount = 0;
        this.hostName = null;
        this.hostPhoneNumber = null;
        this.bloodRequirements = new HashMap<>();
        bloodTypeTargets.forEach((bloodType, target) ->
                bloodRequirements.put(bloodType, new BloodTypeRequirement(bloodType, target))
        );
    }
    public boolean canDonateBloodType(String bloodType) {
        return bloodRequirements.containsKey(bloodType);
    }

    public void recordDonation(String bloodType, double amount) {
        BloodTypeRequirement requirement = bloodRequirements.get(bloodType);
        if (requirement != null) {
            requirement.addDonation(amount);
        }
    }

    public double getTotalTargetAmount() {
        return bloodRequirements.values().stream()
                .mapToDouble(BloodTypeRequirement::getTargetAmount)
                .sum();
    }

    public double getTotalCollectedAmount() {
        return bloodRequirements.values().stream()
                .mapToDouble(BloodTypeRequirement::getCollectedAmount)
                .sum();
    }

    public Map<String, BloodTypeRequirement> getBloodRequirements() {
        return new HashMap<>(bloodRequirements);
    }

    public int getDonorCount() { return donorCount; }
    public void setDonorCount(int donorCount) { this.donorCount = donorCount; }
    public int getVolunteerCount() { return volunteerCount; }
    public void setVolunteerCount(int volunteerCount) { this.volunteerCount = volunteerCount; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getHostPhoneNumber() { return hostPhoneNumber; }
    public void setHostPhoneNumber(String hostPhoneNumber) { this.hostPhoneNumber = hostPhoneNumber; }

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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }


    public double getCurrentBloodCollected() {
        return currentBloodCollected;
    }

    public void setCurrentBloodCollected(double currentBloodCollected) {
        this.currentBloodCollected = currentBloodCollected;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}