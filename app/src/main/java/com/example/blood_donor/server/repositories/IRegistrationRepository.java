package com.example.blood_donor.server.repositories;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.donation.RegistrationType;

public interface IRegistrationRepository {
    void register(String userId, String eventId, RegistrationType type) throws AppException;
    boolean isRegistered(String userId, String eventId) throws AppException;
    int getRegistrationCount(String eventId, RegistrationType type) throws AppException;
}
