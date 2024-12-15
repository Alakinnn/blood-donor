package com.example.blood_donor.server.repositories;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.location.Location;

import java.util.Optional;

public interface ILocationRepository {
    Optional<Location> save(Location location) throws AppException;
    Optional<Location> findById(String id) throws AppException;
    boolean delete(String id) throws AppException;
}
