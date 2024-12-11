package com.example.blood_donor.models.location;

public class Location {
    private String locationId;
    private String address;
    private double latitude;
    private double longitude;
    private String description;

    // Constructor
    public Location(String locationId, String address, double latitude,
                    double longitude, String description) {
        this.locationId = locationId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    // Getters and setters
    // ... (all standard getters and setters)
}

