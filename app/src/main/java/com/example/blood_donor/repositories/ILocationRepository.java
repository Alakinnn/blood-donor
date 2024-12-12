package com.example.blood_donor.repositories;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.location.Location;

import java.util.Optional;

public interface ILocationRepository {
    Optional<Location> save(Location location) throws AppException;
    Optional<Location> findById(String id) throws AppException;
    boolean delete(String id) throws AppException;
}
