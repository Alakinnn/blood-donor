package com.example.blood_donor.server.services;

import android.util.Base64;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.services.interfaces.IAuthService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class AuthService implements IAuthService {
    private static final int SALT_LENGTH = 16;
    private final SecureRandom random;

    public AuthService() {
        this.random = new SecureRandom();
    }

    @Override
    public String hashPassword(String password) throws AppException {
        try {
            // Generate a random salt
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Create MessageDigest instance for SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Add salt to digest
            md.update(salt);

            // Get the hash
            byte[] hash = md.digest(password.getBytes());

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            // Convert to base64 string for storage using Android's Base64
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error hashing password: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPassword(String password, String storedHash) throws AppException {
        try {
            // Decode the stored hash using Android's Base64
            byte[] combined = Base64.decode(storedHash, Base64.NO_WRAP);

            // Extract salt and hash
            byte[] salt = new byte[SALT_LENGTH];
            byte[] hash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.length);

            // Hash the input password with the same salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] newHash = md.digest(password.getBytes());

            // Compare the hashes
            return MessageDigest.isEqual(hash, newHash);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error verifying password: " + e.getMessage());
        }
    }

    @Override
    public String generateToken(User user) throws AppException {
        // Simple UUID-based token generation
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean verifyToken(String token) throws AppException {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}