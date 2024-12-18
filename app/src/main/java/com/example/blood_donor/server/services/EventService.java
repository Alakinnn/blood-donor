package com.example.blood_donor.server.services;

import android.os.Build;

import com.example.blood_donor.server.dto.events.BloodTypeProgress;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.location.Location;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.ILocationRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;
import com.example.blood_donor.server.services.interfaces.IEventService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventService implements IEventService {
    private final IEventRepository eventRepository;
    private final ILocationRepository locationRepository;
    private final IUserRepository userRepository;
    private final IRegistrationRepository registrationRepository;
    private final EventCacheService cacheService;
    private static final LocalTime DEFAULT_DONATION_START = LocalTime.of(9, 0);  // 9 AM
    private static final LocalTime DEFAULT_DONATION_END = LocalTime.of(17, 0);   // 5 PM

    public EventService(IEventRepository eventRepository,
                        EventCacheService cacheService,
                        ILocationRepository locationRepository,
                        IUserRepository userRepository, IRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.cacheService = cacheService;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.registrationRepository = registrationRepository;
    }

    public ApiResponse<DonationEvent> createEvent(String hostId, CreateEventDTO dto) {
        try {
            // Validate blood type targets
            if (dto.getBloodTypeTargets() == null || dto.getBloodTypeTargets().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Blood type targets are required");
            }

            boolean hasAtLeastOneTarget = false;
            // Validate each blood type
            for (Map.Entry<String, Double> entry : dto.getBloodTypeTargets().entrySet()) {
                if (!isValidBloodType(entry.getKey())) {
                    throw new AppException(ErrorCode.INVALID_INPUT,
                            "Invalid blood type: " + entry.getKey());
                }
                if (entry.getValue() < 0) {  // Only check for negative values
                    throw new AppException(ErrorCode.INVALID_INPUT,
                            "Target amount cannot be negative for " + entry.getKey());
                }
                if (entry.getValue() > 0) {
                    hasAtLeastOneTarget = true;
                }
            }

            // Check that at least one blood type has a positive target
            if (!hasAtLeastOneTarget) {
                throw new AppException(ErrorCode.INVALID_INPUT,
                        "At least one blood type must have a target amount greater than 0");
            }

            // Create location and event
            Location location = new Location.Builder()
                    .locationId(UUID.randomUUID().toString())
                    .address(dto.getAddress())
                    .coordinates(dto.getLatitude(), dto.getLongitude())
                    .description(dto.getLocationDescription())
                    .build();

            locationRepository.save(location);

            DonationEvent event = new DonationEvent(
                    UUID.randomUUID().toString(),
                    dto.getTitle(),
                    dto.getDescription(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    location,
                    dto.getBloodTypeTargets(),
                    hostId,
                    dto.getDonationStartTime(),
                    dto.getDonationEndTime()
            );

            eventRepository.save(event);
            return ApiResponse.success(event);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    private boolean isValidBloodType(String bloodType) {
        return Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                .contains(bloodType);
    }

    @Override
    public ApiResponse<List<EventSummaryDTO>> getEventSummaries(EventQueryDTO query) {
        try {
            List<DonationEvent> events = eventRepository.findEvents(query);
            List<EventSummaryDTO> summaries = events.stream()
                    .map(event -> {
                        EventSummaryDTO summary = new EventSummaryDTO();
                        summary.setEventId(event.getEventId());
                        summary.setTitle(event.getTitle());
                        summary.setLatitude(event.getLocation().getLatitude());
                        summary.setLongitude(event.getLocation().getLongitude());
                        summary.setStartTime(event.getStartTime());
                        summary.setEndTime(event.getEndTime());

                        // Convert blood requirements to progress list
                        List<BloodTypeProgress> bloodProgress = event.getBloodRequirements().values().stream()
                                .map(req -> new BloodTypeProgress(
                                        req.getTargetAmount(),
                                        req.getCollectedAmount()
                                ))
                                .collect(Collectors.toList());
                        summary.setBloodProgress(bloodProgress);

                        if (event.getDistance() != null) {
                            summary.setDistance(event.getDistance());
                        }
                        return summary;
                    })
                    .collect(Collectors.toList());

            return ApiResponse.success(summaries);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<EventDetailDTO> getEventDetails(String eventId, String userId) {
        try {
            DonationEvent event = cacheService.getCachedEventDetails(eventId)
                    .orElseGet(() -> {
                        try {
                            return eventRepository.findById(eventId)
                                    .orElseThrow(() -> new AppException(
                                            ErrorCode.INVALID_INPUT,
                                            "Event not found"
                                    ));
                        } catch (AppException e) {
                            throw new RuntimeException(e);
                        }
                    });

            User host = userRepository.findById(event.getHostId())
                    .orElseThrow(() -> new AppException(
                            ErrorCode.DATABASE_ERROR,
                            "Host not found"
                    ));

            int donorCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.DONOR);
            int volunteerCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.VOLUNTEER);

            // Create blood progress list
            List<BloodTypeProgress> bloodProgress = event.getBloodRequirements().values().stream()
                    .map(req -> new BloodTypeProgress(
                            req.getTargetAmount(),
                            req.getCollectedAmount()
                    ))
                    .collect(Collectors.toList());

            EventDetailDTO dto = new EventDetailDTO(
                    event.getEventId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getStartTime(),
                    event.getEndTime(),
                    event.getStatus(),
                    host.getUserId(),
                    host.getFullName(),
                    host.getPhoneNumber(),
                    event.getLocation().getAddress(),
                    event.getLocation().getLatitude(),
                    event.getLocation().getLongitude(),
                    event.getLocation().getDescription(),
                    new ArrayList<>(event.getBloodRequirements().keySet()),
                    event.getTotalTargetAmount(),
                    event.getTotalCollectedAmount(),
                    donorCount,
                    volunteerCount,
                    bloodProgress,
                    getDonationStartTime(event),
                    getDonationEndTime(event)
            );

            return ApiResponse.success(dto);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    // Helper methods for donation hours
    private LocalTime getDonationStartTime(DonationEvent event) {
        return event.getDonationStartTime() != null ?
                event.getDonationStartTime() : DEFAULT_DONATION_START;
    }

    private LocalTime getDonationEndTime(DonationEvent event) {
        return event.getDonationEndTime() != null ?
                event.getDonationEndTime() : DEFAULT_DONATION_END;
    }
    private EventSummaryDTO convertToSummary(DonationEvent event) {
        EventSummaryDTO summary = new EventSummaryDTO();
        // Map fields from event to summary
        summary.setEventId(event.getEventId());
        summary.setTitle(event.getTitle());
        summary.setLatitude(event.getLocation().getLatitude());
        summary.setLongitude(event.getLocation().getLongitude());
        summary.setStartTime(event.getStartTime());
        summary.setEndTime(event.getEndTime());
        summary.setCurrentBloodCollected(event.getTotalCollectedAmount());
        if (event.getDistance() != null) {
            summary.setDistance(event.getDistance());
        }
        return summary;
    }

    private EventSummaryDTO convertToEventSummary(DonationEvent event) {
        EventSummaryDTO summary = new EventSummaryDTO();
        summary.setEventId(event.getEventId());
        summary.setTitle(event.getTitle());
        summary.setLatitude(event.getLocation().getLatitude());
        summary.setLongitude(event.getLocation().getLongitude());
        summary.setStartTime(event.getStartTime());
        summary.setEndTime(event.getEndTime());
        summary.setBloodGoal(event.getTotalTargetAmount());
        summary.setCurrentBloodCollected(event.getTotalCollectedAmount());

        // Set required blood types
        summary.setRequiredBloodTypes(
                new ArrayList<>(event.getBloodRequirements().keySet())
        );

        // Set donation hours
        summary.setDonationStartTime(getDonationStartTime(event));
        summary.setDonationEndTime(getDonationEndTime(event));

        // Set blood progress
        List<BloodTypeProgress> bloodProgress = event.getBloodRequirements()
                .values().stream()
                .map(req -> new BloodTypeProgress(
                        req.getTargetAmount(),
                        req.getCollectedAmount()
                ))
                .collect(Collectors.toList());
        summary.setBloodProgress(bloodProgress);

        if (event.getDistance() != null) {
            summary.setDistance(event.getDistance());
        }

        return summary;
    }

    public ApiResponse<List<EventSummaryDTO>> convertToEventSummaries(List<DonationEvent> events) {
        try {
            List<EventSummaryDTO> summaries = events.stream()
                    .map(this::convertToEventSummary)  // Use our new method
                    .collect(Collectors.toList());

            return ApiResponse.success(summaries);
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ApiResponse<List<EventMarkerDTO>> getEventMarkers(EventQueryDTO query) {
        try {
            // First get the event summaries
            ApiResponse<List<EventSummaryDTO>> response = getEventSummaries(query);

            if (!response.isSuccess()) {
                return ApiResponse.error(response.getErrorCode(), response.getMessage());
            }

            // Convert EventSummaryDTO to EventMarkerDTO
            List<EventMarkerDTO> markers = response.getData().stream()
                    .map(event -> new EventMarkerDTO(
                            event.getEventId(),
                            event.getTitle(),
                            event.getAddress(),
                            event.getStartTime(),
                            event.getEndTime(),
                            event.getRequiredBloodTypes(),
                            event.getBloodGoal(),
                            event.getCurrentBloodCollected(),
                            event.getRegisteredDonors(),
                            event.getLatitude(),
                            event.getLongitude()
                    ))
                    .collect(Collectors.toList());

            return ApiResponse.success(markers);
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
