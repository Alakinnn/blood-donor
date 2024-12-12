package com.example.blood_donor.services;

import android.location.Address;
import android.location.Geocoder;

import com.example.blood_donor.models.location.LatLng;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.location.BoundingBox;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class LocationService {
    private final Geocoder geocoder;
    private static final double EARTH_RADIUS = 6371; // kilometers

    public LocationService(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    public Optional<LatLng> geocodeAddress(String address) throws AppException {
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                return Optional.of(new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                ));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Geocoding failed: " + e.getMessage());
        }
    }

    public boolean isValidLocation(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    public BoundingBox calculateBoundingBox(double latitude, double longitude,
                                            double radiusKm) {
        // Calculate approx. degrees per km at this latitude
        double latRadian = Math.toRadians(latitude);
        double degreesLatPerKm = 1 / 110.574;
        double degreesLngPerKm = 1 / (111.320 * Math.cos(latRadian));

        // Calculate the bounding box
        double latChange = radiusKm * degreesLatPerKm;
        double lngChange = radiusKm * degreesLngPerKm;

        return new BoundingBox(
                latitude - latChange,  // minLat
                latitude + latChange,  // maxLat
                longitude - lngChange, // minLng
                longitude + lngChange  // maxLng
        );
    }

    public double calculateDistance(double lat1, double lon1,
                                    double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * c;
    }
}