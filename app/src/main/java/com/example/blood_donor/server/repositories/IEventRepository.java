package com.example.blood_donor.server.repositories;

import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.events.UpdateEventDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.event.DonationEvent;

import java.util.List;
import java.util.Optional;

public interface IEventRepository {
    List<DonationEvent> findVisibleEvents(EventQueryDTO query) throws AppException;
    List<DonationEvent> findAllEvents(EventQueryDTO query) throws AppException;
    Optional<DonationEvent> findById(String eventId) throws AppException;
    int countEvents(EventQueryDTO query) throws AppException;
    Optional<DonationEvent> save(DonationEvent event) throws AppException;
    List<DonationEvent> findEventsBetween(long startTime, long endTime) throws AppException;
    List<DonationEvent> findEventsByHostId(String hostId) throws AppException;
    List<EventSummaryDTO> findJoinedEvents(String userId) throws AppException;
    List<EventSummaryDTO> findManagedEvents(String userId) throws AppException;
    Optional<DonationEvent> updateEvent(String eventId, UpdateEventDTO updateDto) throws AppException;
}
