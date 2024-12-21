package com.example.blood_donor.server.dto.events;

import java.time.LocalTime;
import java.util.Map;

public class UpdateEventDTO {
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private String address;
    private double latitude;
    private double longitude;
    private Map<String, Double> bloodTypeTargets;
    private LocalTime donationStartTime;
    private LocalTime donationEndTime;

    // Constructor
    public UpdateEventDTO() {}

    // Copy constructor from EventDetailDTO
    public UpdateEventDTO(EventDetailDTO detailDTO) {
        this.title = detailDTO.getTitle();
        this.description = detailDTO.getDescription();
        this.startTime = detailDTO.getStartTime();
        this.endTime = detailDTO.getEndTime();
        this.address = detailDTO.getAddress();
        this.latitude = detailDTO.getLatitude();
        this.longitude = detailDTO.getLongitude();
        // Note: bloodTypeTargets needs to be calculated from bloodProgress
        this.donationStartTime = detailDTO.getDonationStartTime();
        this.donationEndTime = detailDTO.getDonationEndTime();
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Map<String, Double> getBloodTypeTargets() { return bloodTypeTargets; }
    public void setBloodTypeTargets(Map<String, Double> bloodTypeTargets) {
        this.bloodTypeTargets = bloodTypeTargets;
    }

    public LocalTime getDonationStartTime() { return donationStartTime; }
    public void setDonationStartTime(LocalTime donationStartTime) {
        this.donationStartTime = donationStartTime;
    }

    public LocalTime getDonationEndTime() { return donationEndTime; }
    public void setDonationEndTime(LocalTime donationEndTime) {
        this.donationEndTime = donationEndTime;
    }
}