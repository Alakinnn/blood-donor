package com.example.blood_donor.services.interfaces;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.user.User;

public interface IAuthService {
    String hashPassword(String password) throws AppException;
    boolean verifyPassword(String password, String hashedPassword) throws AppException;
    String generateToken(User user) throws AppException;
    boolean verifyToken(String token) throws AppException;
}
