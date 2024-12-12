package com.example.blood_donor.models.location;

class Coordinates {
    private final double latitude;
    private final double longitude;

    private Coordinates(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Coordinates of(double latitude, double longitude) {
        return new Coordinates(latitude, longitude);
    }

    private void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
    }

    private void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


}