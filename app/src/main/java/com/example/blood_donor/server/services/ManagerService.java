package com.example.blood_donor.server.services;

import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManagerService {
    private final IEventRepository eventRepository;
    private final IUserRepository userRepository;
    private final IRegistrationRepository registrationRepository;
    private final EventService eventService;

    public ManagerService(
            IEventRepository eventRepository,
            IUserRepository userRepository,
            IRegistrationRepository registrationRepository,
            EventService eventService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.eventService = eventService;
    }

    public ApiResponse<List<EventSummaryDTO>> getManagerEvents(String managerId, EventQueryDTO query) {
        try {
            // Validate manager exists and has correct role
            Optional<User> manager = userRepository.findById(managerId);
            if (!manager.isPresent() || manager.get().getUserType() != UserType.SITE_MANAGER) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Invalid manager ID");
            }

            // Get all events for this manager
            List<DonationEvent> allEvents = eventRepository.findEventsByHostId(managerId);

            // Apply filters from query
            List<DonationEvent> filteredEvents = filterEvents(allEvents, query);

            // Sort events based on query parameters
            sortEvents(filteredEvents, query);

            // Apply pagination
            int startIndex = (query.getPage() - 1) * query.getPageSize();
            int endIndex = Math.min(startIndex + query.getPageSize(), filteredEvents.size());
            List<DonationEvent> pagedEvents = filteredEvents.subList(startIndex, endIndex);

            // Convert to DTOs using existing event service
            return eventService.convertToEventSummaries(pagedEvents);

        } catch (AppException e) {
            return ApiResponse.error(e.getErrorCode(), e.getMessage());
        }
    }

    private List<DonationEvent> filterEvents(List<DonationEvent> events, EventQueryDTO query) {
        return events.stream()
                .filter(event -> matchesSearchCriteria(event, query))
                .filter(event -> matchesBloodTypes(event, query))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchCriteria(DonationEvent event, EventQueryDTO query) {
        if (query.getSearchTerm() == null || query.getSearchTerm().isEmpty()) {
            return true;
        }
        String searchTerm = query.getSearchTerm().toLowerCase();
        return event.getTitle().toLowerCase().contains(searchTerm) ||
                event.getDescription().toLowerCase().contains(searchTerm) ||
                event.getLocation().getAddress().toLowerCase().contains(searchTerm);
    }

    private boolean matchesBloodTypes(DonationEvent event, EventQueryDTO query) {
        if (query.getBloodTypes() == null || query.getBloodTypes().isEmpty()) {
            return true;
        }
        return query.getBloodTypes().stream()
                .anyMatch(bloodType -> event.getBloodRequirements().containsKey(bloodType));
    }

    private void sortEvents(List<DonationEvent> events, EventQueryDTO query) {
        if (query.getSortBy() == null) return;

        switch (query.getSortBy().toLowerCase()) {
            case "date":
                events.sort((e1, e2) -> query.getSortOrder().equalsIgnoreCase("desc") ?
                        Long.compare(e2.getStartTime(), e1.getStartTime()) :
                        Long.compare(e1.getStartTime(), e2.getStartTime()));
                break;
            case "title":
                events.sort((e1, e2) -> query.getSortOrder().equalsIgnoreCase("desc") ?
                        e2.getTitle().compareTo(e1.getTitle()) :
                        e1.getTitle().compareTo(e2.getTitle()));
                break;
            case "progress":
                events.sort((e1, e2) -> query.getSortOrder().equalsIgnoreCase("desc") ?
                        Double.compare(e2.getTotalCollectedAmount() / e2.getTotalTargetAmount(),
                                e1.getTotalCollectedAmount() / e1.getTotalTargetAmount()) :
                        Double.compare(e1.getTotalCollectedAmount() / e1.getTotalTargetAmount(),
                                e2.getTotalCollectedAmount() / e2.getTotalTargetAmount()));
                break;
        }
    }
}
