package com.example.blood_donor.repositories;

import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.event.DonationEvent;

import java.util.List;
import java.util.Optional;

public interface IEventRepository {
    List<DonationEvent> findEvents(EventQueryDTO query) throws AppException;
    Optional<DonationEvent> findById(String eventId) throws AppException;
    int countEvents(EventQueryDTO query) throws AppException;
}
