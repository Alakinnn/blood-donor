package com.example.blood_donor.services;

import com.example.blood_donor.models.donation.RegistrationType;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.event.EventStatus;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.repositories.RegistrationRepository;

public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            IUserRepository userRepository,
            IEventRepository eventRepository
    ) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    private void validateEventStatus(DonationEvent event) throws AppException {
        // Check if event has expired
        if (event.getEndTime() < System.currentTimeMillis()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Event has expired");
        }

        // Check event status
        EventStatus status = event.getStatus();
        if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    String.format("Cannot register for %s event", status.toString().toLowerCase())
            );
        }

        // Optionally, you might want to only allow registration for specific statuses
        if (status != EventStatus.UPCOMING && status != EventStatus.IN_PROGRESS) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Registration is only allowed for upcoming or in-progress events"
            );
        }
    }

    public ApiResponse<Boolean> register(String userId, String eventId) {
        try {
            // Validate input
            if (userId == null || eventId == null) {
                throw new AppException(ErrorCode.INVALID_INPUT, "User ID and Event ID are required");
            }

            // Check if already registered first
            if (registrationRepository.isRegistered(userId, eventId)) {
                return ApiResponse.success(true);
            }

            // Get user and validate
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "User not found"));

            // Get event and validate
            DonationEvent event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Event not found"));

            // Validate event status
            validateEventStatus(event);

            // Determine registration type based on user type
            RegistrationType type = (user.getUserType() == UserType.SITE_MANAGER)
                    ? RegistrationType.VOLUNTEER
                    : RegistrationType.DONOR;

            // Register the user
            registrationRepository.register(userId, eventId, type);

            // Return success with false to indicate new registration
            return ApiResponse.success(false);

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred: " + e.getMessage()
            );
        }
    }
}