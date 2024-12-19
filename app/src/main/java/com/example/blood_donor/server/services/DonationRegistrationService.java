package com.example.blood_donor.server.services;

import com.example.blood_donor.server.models.donation.Registration;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;

public class DonationRegistrationService {
    private static final double STANDARD_DONATION_AMOUNT = 0.45; // Standard blood donation amount in liters
    private final IRegistrationRepository registrationRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    public DonationRegistrationService(
            IRegistrationRepository registrationRepository,
            IUserRepository userRepository,
            IEventRepository eventRepository
    ) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
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

            // For donors, validate blood type
            if (type == RegistrationType.DONOR) {
                if (user.getBloodType() == null) {
                    throw new AppException(ErrorCode.INVALID_INPUT, "Blood type is required for donors");
                }
                if (!event.canDonateBloodType(user.getBloodType())) {
                    throw new AppException(ErrorCode.INVALID_INPUT,
                            "Your blood type is not required for this event");
                }
            }

            // Register the participant
            registrationRepository.register(userId, eventId, type);

            // If donor, record the donation amount
            if (type == RegistrationType.DONOR) {
                event.recordDonation(user.getBloodType(), STANDARD_DONATION_AMOUNT);
                eventRepository.save(event);
            }

            return ApiResponse.success(false); // false indicates new registration
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
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

        // Only allow registration for upcoming or in-progress events
        if (status != EventStatus.UPCOMING && status != EventStatus.IN_PROGRESS) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Registration is only allowed for upcoming or in-progress events"
            );
        }
    }
}