package com.example.blood_donor.server.services;

import android.annotation.SuppressLint;

import com.example.blood_donor.server.dto.events.BloodTypeProgress;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.PagedResults;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.location.Location;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.ILocationRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.services.interfaces.IEventService;
import com.example.blood_donor.ui.manager.ServiceLocator;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EventService implements IEventService {
    private final IEventRepository eventRepository;
    private final ILocationRepository locationRepository;
    private final IUserRepository userRepository;
    private final IRegistrationRepository registrationRepository;
    private final EventCacheService eventCacheService;
    private final CacheService cacheService;
    private static final LocalTime DEFAULT_DONATION_START = LocalTime.of(9, 0);  // 9 AM
    private static final LocalTime DEFAULT_DONATION_END = LocalTime.of(17, 0);   // 5 PM

    public EventService(IEventRepository eventRepository,
                        EventCacheService eventCacheService,
                        ILocationRepository locationRepository,
                        IUserRepository userRepository, IRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.eventCacheService = eventCacheService;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.registrationRepository = registrationRepository;
        this.cacheService = ServiceLocator.getCacheService();
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

    public void cacheEventDetails(EventSummaryDTO summary) {
        // Convert summary to full event details
        EventDetailDTO details = new EventDetailDTO(
                summary.getEventId(),
                summary.getTitle(),
                null, // Description will be loaded from DB
                summary.getStartTime(),
                summary.getEndTime(),
                null, // Status will be determined from times
                null, // Host info will be loaded
                null, // Host name will be loaded
                null, // Host phone will be loaded
                summary.getAddress(),
                summary.getLatitude(),
                summary.getLongitude(),
                null, // Location description will be loaded
                summary.getRequiredBloodTypes(),
                summary.getBloodGoal(),
                summary.getCurrentBloodCollected(),
                0, // Donor count will be loaded
                0, // Volunteer count will be loaded
                summary.getBloodProgress(),
                summary.getDonationStartTime(),
                summary.getDonationEndTime()
        );

        eventCacheService.cacheEventDetails(summary.getEventId(), details);
    }

    @Override
    public ApiResponse<PagedResults<EventSummaryDTO>> getEventSummaries(EventQueryDTO query) {
        @SuppressLint("DefaultLocale") String cacheKey = String.format("events_page_%d_size_%d_%s_%s",
                query.getPage(),
                query.getPageSize(),
                query.getSortBy(),
                query.getSearchTerm() != null ? query.getSearchTerm() : "none"
        );

        PagedResults<EventSummaryDTO> cached = cacheService.get(cacheKey, PagedResults.class);
        if (cached != null) {
            return ApiResponse.success(cached);
        }

        try {
            List<DonationEvent> events = eventRepository.findEvents(query);
            int totalCount = eventRepository.countEvents(query);

            List<EventSummaryDTO> summaries = events.stream()
                    .map(this::convertToEventSummary)
                    .collect(Collectors.toList());

            PagedResults<EventSummaryDTO> results = new PagedResults<>(
                    summaries,
                    totalCount,
                    query.getPage(),
                    query.getPageSize()
            );

            // Cache with shorter TTL since event list changes more frequently
            cacheService.put(cacheKey, results, TimeUnit.MINUTES.toMillis(1));
            return ApiResponse.success(results);
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public ApiResponse<EventDetailDTO> getEventDetails(String eventId, String userId) {
        try {
            if (eventId == null) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event ID cannot be null");
            }

            // Check main cache first
            String cacheKey = CacheKeys.eventKey(eventId);
            EventDetailDTO cached = cacheService.get(cacheKey, EventDetailDTO.class);
            if (cached != null) {
                return ApiResponse.success(cached);
            }

            // Load from database
            Optional<DonationEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event not found");
            }

            DonationEvent event = eventOpt.get();

            // Load host information
            String hostId = event.getHostId();
            if (hostId == null) {
                return ApiResponse.error(ErrorCode.DATABASE_ERROR, "Event has no host information");
            }

            Optional<User> hostOpt = userRepository.findById(hostId);
            if (!hostOpt.isPresent()) {
                return ApiResponse.error(ErrorCode.DATABASE_ERROR, "Host information not found");
            }

            User host = hostOpt.get();

            // Get registration counts
            int donorCount = 0;
            int volunteerCount = 0;
            try {
                donorCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.DONOR);
                volunteerCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.VOLUNTEER);
            } catch (AppException e) {
                // Continue with 0 counts rather than failing
            }

            // Convert blood requirements to progress list
            List<BloodTypeProgress> bloodProgress = event.getBloodRequirements().values().stream()
                    .map(req -> new BloodTypeProgress(req.getTargetAmount(), req.getCollectedAmount()))
                    .collect(Collectors.toList());

            // Create full event details
            EventDetailDTO details = new EventDetailDTO(
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
                    event.getDonationStartTime(),
                    event.getDonationEndTime()
            );

            // Cache the result
            cacheService.put(cacheKey, details);

            return ApiResponse.success(details);

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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
            List<DonationEvent> events = eventRepository.findEvents(query);

            List<EventMarkerDTO> markers = events.stream()
                    .map(event -> {
                        try {
                            return new EventMarkerDTO(
                                    event.getEventId(),
                                    event.getTitle(),
                                    event.getLocation().getAddress(),
                                    event.getStartTime(),
                                    event.getEndTime(),
                                    new ArrayList<>(event.getBloodRequirements().keySet()),
                                    event.getTotalTargetAmount(),
                                    event.getTotalCollectedAmount(),
                                    registrationRepository.getRegistrationCount(event.getEventId(), RegistrationType.DONOR),
                                    event.getLocation().getLatitude(),
                                    event.getLocation().getLongitude()
                            );
                        } catch (AppException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            return ApiResponse.success(markers);
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
//    TODO: the registratioRepository doesnt have the getUserEvent related stuff
//    public ApiResponse<PagedResults<EventSummaryDTO>> getUserEventHistory(String userId, int page, int pageSize) {
//        String cacheKey = String.format("user_%s_history_page_%d", userId, page);
//
//        PagedResults<EventSummaryDTO> cached = cacheService.get(cacheKey, PagedResults.class);
//        if (cached != null) {
//            return ApiResponse.success(cached);
//        }
//
//        try {
//            // Get user's event registrations
//            List<String> eventIds = registrationRepository.getUserEventIds(userId, page, pageSize);
//            int totalCount = registrationRepository.countUserEvents(userId);
//
//            // Get event details for each registration
//            List<EventSummaryDTO> summaries = eventIds.stream()
//                    .map(eventId -> {
//                        Optional<DonationEvent> event = eventRepository.findById(eventId);
//                        return event.map(this::convertToEventSummary).orElse(null);
//                    })
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//
//            PagedResults<EventSummaryDTO> results = new PagedResults<>(
//                    summaries,
//                    totalCount,
//                    page,
//                    pageSize
//            );
//
//            cacheService.put(cacheKey, results, TimeUnit.MINUTES.toMillis(5));
//            return ApiResponse.success(results);
//        } catch (Exception e) {
//            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }

}
