package com.example.blood_donor.services;

import com.example.blood_donor.dto.events.CreateEventDTO;
import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.dto.locations.EventSummaryDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.ILocationRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.interfaces.IEventService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventService implements IEventService {
    private final IEventRepository eventRepository;
    private final ILocationRepository locationRepository;
    private final IUserRepository userRepository;
    private final EventCacheService cacheService;


    public EventService(IEventRepository eventRepository,
                        EventCacheService cacheService,
                        ILocationRepository locationRepository,
                        IUserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.cacheService = cacheService;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    public ApiResponse<DonationEvent> createEvent(String hostId, CreateEventDTO dto) {
        try {
            // Validate host exists and is a site manager
            User host = userRepository.findById(hostId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Host not found"));

            if (host.getUserType() != UserType.SITE_MANAGER) {
                throw new AppException(ErrorCode.INVALID_INPUT,
                        "Only site managers can create events");
            }

            // Create location first
            Location location = new Location.Builder()
                    .locationId(UUID.randomUUID().toString())
                    .address(dto.getAddress())
                    .coordinates(dto.getLatitude(), dto.getLongitude())
                    .description(dto.getLocationDescription())
                    .build();

            locationRepository.save(location);

            // Create event
            DonationEvent event = new DonationEvent(
                    UUID.randomUUID().toString(),
                    dto.getTitle(),
                    dto.getDescription(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    location,
                    dto.getRequiredBloodTypes(),
                    dto.getBloodGoal(),
                    hostId
            );

            eventRepository.save(event);
            return ApiResponse.success(event);

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ApiResponse<List<EventSummaryDTO>> getEventSummaries(EventQueryDTO query) {
        try {
            List<DonationEvent> events = eventRepository.findEvents(query);
            List<EventSummaryDTO> summaries = events.stream()
                    .map(this::convertToSummary)
                    .collect(Collectors.toList());
            return ApiResponse.success(summaries);
        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public ApiResponse<DonationEvent> getEventDetails(String eventId) {
        try {
            // Check cache first
            Optional<DonationEvent> cachedEvent = cacheService.getCachedEventDetails(eventId);
            if (cachedEvent.isPresent()) {
                return ApiResponse.success(cachedEvent.get());
            }

            // If not in cache, get from repository
            Optional<DonationEvent> event = eventRepository.findById(eventId);
            if (event.isPresent()) {
                // Cache the result
                cacheService.cacheEventDetails(eventId, event.get());
                return ApiResponse.success(event.get());
            }

            return ApiResponse.error(ErrorCode.INVALID_INPUT, "Event not found");
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
        summary.setRequiredBloodTypes(event.getRequiredBloodTypes());
        summary.setStartTime(event.getStartTime());
        summary.setEndTime(event.getEndTime());
        summary.setBloodGoal(event.getBloodGoal());
        summary.setCurrentBloodCollected(event.getCurrentBloodCollected());
        if (event.getDistance() != null) {
            summary.setDistance(event.getDistance());
        }
        return summary;
    }
}
