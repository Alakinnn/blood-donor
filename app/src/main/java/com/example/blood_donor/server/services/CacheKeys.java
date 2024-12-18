package com.example.blood_donor.server.services;

public class CacheKeys {
    public static final String EVENT_PREFIX = "event";
    public static final String USER_PREFIX = "user";
    public static final String REGISTRATION_PREFIX = "registration";

    public static String eventKey(String eventId) {
        return CacheService.generateKey(EVENT_PREFIX, eventId);
    }

    public static String userKey(String userId) {
        return CacheService.generateKey(USER_PREFIX, userId);
    }

    public static String registrationKey(String registrationId) {
        return CacheService.generateKey(REGISTRATION_PREFIX, registrationId);
    }
}

