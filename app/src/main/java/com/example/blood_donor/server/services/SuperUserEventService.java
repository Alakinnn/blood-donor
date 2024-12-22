package com.example.blood_donor.server.services;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.donation.Registration;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.ILocationRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.ui.manager.NotificationManager;
import com.example.blood_donor.ui.manager.ServiceLocator;

import java.util.List;
import java.util.Optional;

public class SuperUserEventService {
    private final IEventRepository eventRepository;
    private final IRegistrationRepository registrationRepository;
    private final DatabaseHelper dbHelper;
    private final IUserRepository userRepository;
    private final EventService eventService;

    public SuperUserEventService(IEventRepository eventRepository,
                                 IRegistrationRepository registrationRepository,
                                 DatabaseHelper dbHelper,
                                 IUserRepository userRepository,
                                 EventService eventService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.registrationRepository = registrationRepository;
        this.dbHelper = dbHelper;
    }

    public ApiResponse<DonationEvent> createEventForManager(String managerEmail, CreateEventDTO dto) {
        try {
            // Find manager by email
            Optional<User> managerOpt = userRepository.findByEmail(managerEmail);
            if (!managerOpt.isPresent()) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Manager not found");
            }

            User manager = managerOpt.get();
            if (manager.getUserType() != UserType.SITE_MANAGER) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Provided email is not for a site manager");
            }

            // Create event using the manager's ID
            return eventService.createEvent(manager.getUserId(), dto);

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    public ApiResponse<Void> cancelEvent(String eventId) {
        try {
            Optional<DonationEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event not found");
            }

            DonationEvent event = eventOpt.get();
            event.setStatus(EventStatus.CANCELLED);
            eventRepository.save(event);

            // Get all registrations for this event
            List<Registration> registrations = registrationRepository.getEventRegistrations(eventId);

            // Create notification for each participant using the existing notificationManager
            NotificationManager notificationManager = ServiceLocator.getNotificationManager();
            if (notificationManager != null) {
                for (Registration reg : registrations) {
                    notificationManager.createEventNotification(
                            reg.getUserId(),
                            eventId,
                            "Event Cancelled",
                            String.format("The event '%s' has been cancelled", event.getTitle()),
                            NotificationType.EVENT_UPDATE
                    );
                }
            }

            return ApiResponse.success(null);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    public ApiResponse<List<DonationEvent>> findAllEvents(EventQueryDTO query) {
        try {
            // This method will return all events including cancelled ones
            return ApiResponse.success(eventRepository.findAllEvents(query));
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }
}
