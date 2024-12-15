package com.example.blood_donor.server.services;

import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;

public class DonationRegistrationService {
    private static final double STANDARD_DONATION_AMOUNT = 0.45;
    private final RegistrationRepository registrationRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    public DonationRegistrationService(
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

    public ApiResponse<Boolean> registerDonor(String userId, String eventId, String bloodType) {
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

            // Validate blood type requirement
            if (!event.canDonateBloodType(bloodType)) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT,
                        "Blood type " + bloodType + " is not required for this event");
            }

            // Register the donor
            registrationRepository.register(userId, eventId, RegistrationType.DONOR);

            // Record the donation amount
            event.recordDonation(bloodType, STANDARD_DONATION_AMOUNT);
            eventRepository.save(event);

            return ApiResponse.success(true);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }
}