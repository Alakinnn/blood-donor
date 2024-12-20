package com.example.blood_donor.server.services;

import android.os.Build;
import android.util.Log;

import com.example.blood_donor.server.dto.events.BloodTypeProgress;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.BloodTypeRequirement;
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
import java.util.Optional;
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

    public void cacheEventDetails(EventSummaryDTO summary) {
        try {
            // Load complete event from database
            Optional<DonationEvent> eventOpt = eventRepository.findById(summary.getEventId());
            if (!eventOpt.isPresent()) {
                Log.w("EventService", "Event not found in database: " + summary.getEventId());
                return;
            }

            DonationEvent event = eventOpt.get();

            // Load host information
            Optional<User> hostOpt = userRepository.findById(event.getHostId());
            if (!hostOpt.isPresent()) {
                Log.w("EventService", "Host not found for event: " + summary.getEventId());
                return;
            }

            User host = hostOpt.get();

            // Get registration counts
            int donorCount = registrationRepository.getRegistrationCount(
                    summary.getEventId(),
                    RegistrationType.DONOR
            );
            int volunteerCount = registrationRepository.getRegistrationCount(
                    summary.getEventId(),
                    RegistrationType.VOLUNTEER
            );

            // Create complete event details
            EventDetailDTO details = new EventDetailDTO(
                    event.getEventId(),
                    event.getTitle(),
                    event.getDescription(),  // Now populated
                    event.getStartTime(),
                    event.getEndTime(),
                    event.getStatus(),
                    host.getUserId(),        // Now populated
                    host.getFullName(),      // Now populated
                    host.getPhoneNumber(),   // Now populated
                    event.getLocation().getAddress(),
                    event.getLocation().getLatitude(),
                    event.getLocation().getLongitude(),
                    event.getLocation().getDescription(),
                    new ArrayList<>(event.getBloodRequirements().keySet()),
                    event.getTotalTargetAmount(),
                    event.getTotalCollectedAmount(),
                    donorCount,              // Now populated
                    volunteerCount,          // Now populated
                    buildBloodProgress(event.getBloodRequirements()),
                    event.getDonationStartTime(),
                    event.getDonationEndTime()
            );

            cacheService.cacheEventDetails(summary.getEventId(), details);

        } catch (AppException e) {
            Log.e("EventService", "Error caching event details", e);
        }
    }

    private List<BloodTypeProgress> buildBloodProgress(Map<String, BloodTypeRequirement> requirements) {
        return requirements.values().stream()
                .map(req -> new BloodTypeProgress(
                        req.getTargetAmount(),
                        req.getCollectedAmount()
                ))
                .collect(Collectors.toList());
    }
    @Override
    public ApiResponse<List<EventSummaryDTO>> getEventSummaries(EventQueryDTO query) {
        try {
            List<DonationEvent> events = eventRepository.findEvents(query);
            Log.d("EventService", "Found " + events.size() + " events in database");

            List<EventSummaryDTO> summaries = events.stream()
                    .map(event -> {
                        EventSummaryDTO summary = new EventSummaryDTO();

                        // Basic event info
                        summary.setEventId(event.getEventId());
                        summary.setTitle(event.getTitle());
                        summary.setStartTime(event.getStartTime());
                        summary.setEndTime(event.getEndTime());

                        // Location info
                        if (event.getLocation() != null) {
                            summary.setLatitude(event.getLocation().getLatitude());
                            summary.setLongitude(event.getLocation().getLongitude());
                            summary.setAddress(event.getLocation().getAddress());
                        }

                        // Blood requirements
                        Map<String, BloodTypeRequirement> requirements = event.getBloodRequirements();
                        List<BloodTypeProgress> bloodProgress = new ArrayList<>();

                        if (requirements != null && !requirements.isEmpty()) {
                            // Set required blood types
                            summary.setRequiredBloodTypes(new ArrayList<>(requirements.keySet()));

                            // Calculate totals
                            double totalGoal = 0;
                            double totalCollected = 0;

                            // Process each blood type requirement
                            for (Map.Entry<String, BloodTypeRequirement> entry : requirements.entrySet()) {
                                BloodTypeRequirement req = entry.getValue();
                                totalGoal += req.getTargetAmount();
                                totalCollected += req.getCollectedAmount();

                                // Add to progress list
                                bloodProgress.add(new BloodTypeProgress(
                                        req.getTargetAmount(),
                                        req.getCollectedAmount()
                                ));
                            }

                            summary.setBloodGoal(totalGoal);
                            summary.setCurrentBloodCollected(totalCollected);
                            summary.setTotalProgress((totalGoal > 0) ?
                                    (totalCollected / totalGoal) * 100 : 0);
                        }

                        summary.setBloodProgress(bloodProgress);

                        // Donation schedule
                        if (event.getDonationStartTime() != null) {
                            summary.setDonationStartTime(event.getDonationStartTime());
                        }
                        if (event.getDonationEndTime() != null) {
                            summary.setDonationEndTime(event.getDonationEndTime());
                        }

                        // Registration counts
                        try {
                            int donorCount = registrationRepository.getRegistrationCount(
                                    event.getEventId(),
                                    RegistrationType.DONOR
                            );
                            int volunteerCount = registrationRepository.getRegistrationCount(
                                    event.getEventId(),
                                    RegistrationType.VOLUNTEER
                            );
                            summary.setRegisteredDonors(donorCount);
                            summary.setRegisteredVolunteers(volunteerCount);
                        } catch (AppException e) {
                            Log.e("EventService", "Error getting registration counts", e);
                        }

                        // Distance if provided
                        if (event.getDistance() != null) {
                            summary.setDistance(event.getDistance());
                        }

                        return summary;
                    })
                    .collect(Collectors.toList());

            return ApiResponse.success(summaries);
        } catch (AppException e) {
            Log.e("EventService", "Error getting event summaries", e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            Log.e("EventService", "Unexpected error getting event summaries", e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }
    @Override
    public ApiResponse<EventDetailDTO> getEventDetails(String eventId, String userId) {
        try {
            if (eventId == null) {
                Log.e("EventService", "Attempted to get details for null event ID");
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event ID cannot be null");
            }

            Log.d("EventService", "Fetching event with ID: " + eventId);

            // First check cache
            Optional<EventDetailDTO> cachedEvent = cacheService.getCachedEventDetails(eventId);
            if (cachedEvent.isPresent()) {
                EventDetailDTO cached = cachedEvent.get();
                // Only try to load host details if we have a host ID
                if (cached.getHostId() != null && (cached.getHostName() == null || cached.getHostPhoneNumber() == null)) {
                    try {
                        Optional<User> hostOpt = userRepository.findById(cached.getHostId());
                        hostOpt.ifPresent(host -> {
                            cached.setHostName(host.getFullName());
                            cached.setHostPhoneNumber(host.getPhoneNumber());
                        });
                    } catch (AppException e) {
                        Log.w("EventService", "Could not load host details for cached event", e);
                    }
                }
                return ApiResponse.success(cached);
            }

            // Load from database with full error logging
            Optional<DonationEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                Log.w("EventService", "Event not found in database: " + eventId);
                return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event not found");
            }

            DonationEvent event = eventOpt.get();

            // Load host information with null check
            String hostId = event.getHostId();
            if (hostId == null) {
                Log.w("EventService", "Event has no host ID: " + eventId);
                return ApiResponse.error(ErrorCode.DATABASE_ERROR, "Event has no host information");
            }

            Optional<User> hostOpt = userRepository.findById(hostId);
            if (!hostOpt.isPresent()) {
                Log.w("EventService", "Host not found for event: " + eventId + ", host ID: " + hostId);
                return ApiResponse.error(ErrorCode.DATABASE_ERROR, "Host information not found");
            }

            User host = hostOpt.get();

            // Get registration counts with error handling
            int donorCount = 0;
            int volunteerCount = 0;
            try {
                donorCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.DONOR);
                volunteerCount = registrationRepository.getRegistrationCount(eventId, RegistrationType.VOLUNTEER);
            } catch (AppException e) {
                Log.w("EventService", "Error getting registration counts", e);
                // Continue with 0 counts rather than failing
            }

            // Convert blood requirements to progress list
            List<BloodTypeProgress> bloodProgress = event.getBloodRequirements().values().stream()
                    .map(req -> new BloodTypeProgress(
                            req.getTargetAmount(),
                            req.getCollectedAmount()
                    ))
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

            // Cache the details
            cacheService.cacheEventDetails(eventId, details);

            Log.d("EventService", "Successfully created event details for: " + eventId);
            return ApiResponse.success(details);

        } catch (AppException e) {
            Log.e("EventService", "Error getting event details: " + e.getMessage(), e);
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            Log.e("EventService", "Unexpected error getting event details", e);
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

        Map<String, BloodTypeRequirement> requirements = event.getBloodRequirements();
        double totalGoal = 0;
        double totalCollected = 0;

        List<BloodTypeProgress> bloodProgress = new ArrayList<>();

        Log.d("EventService", "Converting event: " + event.getEventId());
        Log.d("EventService", "Blood requirements: " + requirements.toString());

        for (Map.Entry<String, BloodTypeRequirement> entry : requirements.entrySet()) {
            BloodTypeRequirement req = entry.getValue();
            double targetAmount = req.getTargetAmount();
            double collectedAmount = req.getCollectedAmount();

            totalGoal += targetAmount;
            totalCollected += collectedAmount;

            Log.d("EventService", String.format("Blood type %s - Target: %.2f, Collected: %.2f",
                    entry.getKey(), targetAmount, collectedAmount));

            bloodProgress.add(new BloodTypeProgress(targetAmount, collectedAmount));
        }

        summary.setBloodGoal(totalGoal);
        summary.setCurrentBloodCollected(totalCollected);

        double progress = (totalGoal > 0) ? (totalCollected / totalGoal) * 100 : 0;
        summary.setTotalProgress(progress);

        Log.d("EventService", String.format("Total Goal: %.2f, Total Collected: %.2f, Progress: %.2f%%",
                totalGoal, totalCollected, progress));

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
