package com.example.blood_donor.repositories;

import com.example.blood_donor.errors.AppException;

import java.util.Optional;

public interface ISessionRepository {
    void saveSession(String token, String userId) throws AppException;
    Optional<String> getUserIdByToken(String token) throws AppException;
    void deleteSession(String token) throws AppException;
    void deleteAllUserSessions(String userId) throws AppException;
}
