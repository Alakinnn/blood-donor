package com.example.blood_donor.models.location;

public class BoundingBox {
    private final double minLat;
    private final double maxLat;
    private final double minLng;
    private final double maxLng;

    public BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLng = minLng;
        this.maxLng = maxLng;
    }

    public double getMinLat() { return minLat; }
    public double getMaxLat() { return maxLat; }
    public double getMinLng() { return minLng; }
    public double getMaxLng() { return maxLng; }

    public boolean contains(double lat, double lng) {
        return lat >= minLat && lat <= maxLat &&
                lng >= minLng && lng <= maxLng;
    }
}
