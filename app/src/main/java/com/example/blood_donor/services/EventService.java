package com.example.blood_donor.services;

import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.dto.locations.EventSummaryDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.services.interfaces.IEventService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventService implements IEventService {
    private final IEventRepository eventRepository;
    private final EventCacheService cacheService;

    public EventService(IEventRepository eventRepository,
                        EventCacheService cacheService) {
        this.eventRepository = eventRepository;
        this.cacheService = cacheService;
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
