package com.example.blood_donor.validators;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.dto.events.CreateEventDTO;
import com.example.blood_donor.services.LocationService;

public class LocationValidator implements IValidator<CreateEventDTO> {
    private final LocationService locationService;

    public LocationValidator(LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public void validate(CreateEventDTO request) throws AppException {
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Address is required");
        }

        if (!locationService.isValidLocation(
                request.getLatitude(),
                request.getLongitude())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid coordinates");
        }

        // Verify the coordinates match the address (optional but recommended)
        locationService.geocodeAddress(request.getAddress())
                .ifPresent(latLng -> {
                    double MAX_DISTANCE_KM = 1.0; // 1km tolerance
                    double distance = locationService.calculateDistance(
                            latLng.getLatitude(), latLng.getLongitude(),
                            request.getLatitude(), request.getLongitude()
                    );
                    if (distance > MAX_DISTANCE_KM) {
                        throw new RuntimeException("Coordinates don't match address");
                    }
                });
    }
}