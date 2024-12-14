package com.example.blood_donor.repositories;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;

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
