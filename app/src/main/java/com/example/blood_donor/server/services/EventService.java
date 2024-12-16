package com.example.blood_donor.server.services;

import android.os.Build;

import com.example.blood_donor.server.dto.events.BloodTypeProgress;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
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
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;
import com.example.blood_donor.server.services.interfaces.IEventService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventService implements IEventService {
    private final IEventRepository eventRepository;
    private final ILocationRepository locationRepository;
    private final IUserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventCacheService cacheService;

    public EventService(IEventRepository eventRepository,
                        EventCacheService cacheService,
                        ILocationRepository locationRepository,
                        IUserRepository userRepository, RegistrationRepository registrationRepository) {
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

            // Validate each blood type
            for (Map.Entry<String, Double> entry : dto.getBloodTypeTargets().entrySet()) {
                if (!isValidBloodType(entry.getKey())) {
                    throw new AppException(ErrorCode.INVALID_INPUT,
                            "Invalid blood type: " + entry.getKey());
                }
                if (entry.getValue() <= 0) {
                    throw new AppException(ErrorCode.INVALID_INPUT,
                            "Target amount must be greater than 0 for " + entry.getKey());
                }
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
                    dto.getBloodTypeTargets(),  // Pass blood type targets
                    hostId
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

            List<BloodTypeProgress> bloodProgress = event.getBloodRequirements().values().stream()
                    .map(req -> new BloodTypeProgress(
                            req.getTargetAmount(),
                            req.getCollectedAmount()
                    ))
                    .collect(Collectors.toList());

            EventDetailDTO dto = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                dto = new EventDetailDTO(
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
                        event.getBloodRequirements().keySet().stream().toList(),
                        event.getTotalTargetAmount(),
                        event.getTotalCollectedAmount(),
                        donorCount,
                        volunteerCount,
                        bloodProgress
                );
            }

            return ApiResponse.success(dto);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
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
}
