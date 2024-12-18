package com.example.blood_donor.server.services;

import android.util.LruCache;

import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.models.event.DonationEvent;

import java.util.Optional;

public class EventCacheService {
    private static final int CACHE_SIZE = 100;
    private final LruCache<String, EventDetailDTO> detailsCache;

    public EventCacheService() {
        detailsCache = new LruCache<>(CACHE_SIZE);
    }

    public void cacheEventDetails(String eventId, EventDetailDTO details) {
        detailsCache.put(eventId, details);
    }

    public Optional<EventDetailDTO> getCachedEventDetails(String eventId) {
        return Optional.ofNullable(detailsCache.get(eventId));
    }

    public void clearCache() {
        detailsCache.evictAll();
    }
}