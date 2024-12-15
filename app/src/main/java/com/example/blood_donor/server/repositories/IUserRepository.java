package com.example.blood_donor.server.repositories;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.user.User;

import java.util.List;
import java.util.Optional;

public interface IUserRepository {
    Optional<User> createUser(User user) throws AppException;
    Optional<User> findByEmail(String email) throws AppException;
    Optional<User> findById(String id) throws AppException;
    boolean existsByEmail(String email) throws AppException;
    boolean updateUser(User user) throws AppException;
    boolean deleteUser(String id) throws AppException;
    List<User> findUsersByTimeRange(long startTime, long endTime) throws AppException;
}
