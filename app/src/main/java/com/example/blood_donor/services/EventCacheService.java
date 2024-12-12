package com.example.blood_donor.services;

import android.util.LruCache;

import com.example.blood_donor.models.event.DonationEvent;

import java.util.Optional;

public class EventCacheService {
    private static final int CACHE_SIZE = 100;
    private final LruCache<String, DonationEvent> detailsCache;

    public EventCacheService() {
        detailsCache = new LruCache<>(CACHE_SIZE);
    }

    public void cacheEventDetails(String eventId, DonationEvent event) {
        detailsCache.put(eventId, event);
    }

    public Optional<DonationEvent> getCachedEventDetails(String eventId) {
        return Optional.ofNullable(detailsCache.get(eventId));
    }

    public void clearCache() {
        detailsCache.evictAll();
    }
}