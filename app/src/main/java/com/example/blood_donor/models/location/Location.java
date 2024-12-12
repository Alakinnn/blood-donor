package com.example.blood_donor.models.location;

public class Location {
    private final String locationId;
    private final String address;
    private final Coordinates coordinates;
    private final String description;

    private Location(Builder builder) {
        this.locationId = builder.locationId;
        this.address = builder.address;
        this.coordinates = Coordinates.of(builder.latitude, builder.longitude);
        this.description = builder.description;
    }

    // Builder pattern for cleaner object construction
    public static class Builder {
        private String locationId;
        private String address;
        private double latitude;
        private double longitude;
        private String description;

        public Builder locationId(String locationId) {
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Location ID cannot be null or empty");
            }
            this.locationId = locationId;
            return this;
        }

        public Builder address(String address) {
            if (address == null || address.trim().isEmpty()) {
                throw new IllegalArgumentException("Address cannot be null or empty");
            }
            this.address = address;
            return this;
        }

        public Builder coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Location build() {
            validateRequiredFields();
            return new Location(this);
        }

        private void validateRequiredFields() {
            if (locationId == null || address == null) {
                throw new IllegalStateException("Location ID and address are required");
            }
        }
    }

    // Getters
    public String getLocationId() {
        return locationId;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return coordinates.getLatitude();
    }

    public double getLongitude() {
        return coordinates.getLongitude();
    }

    public String getDescription() {
        return description;
    }
}

