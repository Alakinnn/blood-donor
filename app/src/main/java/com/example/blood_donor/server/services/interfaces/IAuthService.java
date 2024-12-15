package com.example.blood_donor.server.services.interfaces;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.user.User;

public interface IAuthService {
    String hashPassword(String password) throws AppException;
    boolean verifyPassword(String password, String hashedPassword) throws AppException;
    String generateToken(User user) throws AppException;
    boolean verifyToken(String token) throws AppException;
}
