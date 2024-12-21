package com.example.blood_donor.server.validators;

import com.example.blood_donor.server.dto.events.UpdateEventDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.services.LocationService;

import java.util.Map;

public class UpdateEventValidator implements IValidator<UpdateEventDTO> {
    private final LocationService locationService;

    public UpdateEventValidator(LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public void validate(UpdateEventDTO request) throws AppException {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Title is required");
        }

        if (request.getEndTime() <= request.getStartTime()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "End time must be after start time");
        }

        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Address is required");
        }

        if (!locationService.isValidLocation(request.getLatitude(), request.getLongitude())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid coordinates");
        }

        validateBloodTypeTargets(request.getBloodTypeTargets());
    }

    private void validateBloodTypeTargets(Map<String, Double> targets) throws AppException {
        if (targets == null || targets.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Blood type targets are required");
        }

        boolean hasPositiveTarget = false;
        for (Map.Entry<String, Double> entry : targets.entrySet()) {
            if (entry.getValue() < 0) {
                throw new AppException(ErrorCode.INVALID_INPUT,
                        "Blood type target cannot be negative: " + entry.getKey());
            }
            if (entry.getValue() > 0) {
                hasPositiveTarget = true;
            }
        }

        if (!hasPositiveTarget) {
            throw new AppException(ErrorCode.INVALID_INPUT,
                    "At least one blood type must have a positive target amount");
        }
    }
}