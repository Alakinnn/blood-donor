package com.example.blood_donor.server.services;

import android.util.LruCache;

import java.util.concurrent.TimeUnit;

public class CacheService {
    private final LruCache<String, CacheEntry> memoryCache;
    private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MB
    private static final long DEFAULT_TTL = TimeUnit.MINUTES.toMillis(5); // 5 minutes default TTL

    private static class CacheEntry {
        final Object data;
        final long expiryTime;

        CacheEntry(Object data, long ttl) {
            this.data = data;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public CacheService() {
        memoryCache = new LruCache<>(CACHE_SIZE);
    }

    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    public void put(String key, Object value, long ttl) {
        if (key == null || value == null) return;
        memoryCache.put(key, new CacheEntry(value, ttl));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired() && type.isInstance(entry.data)) {
            return (T) entry.data;
        }
        if (entry != null) {
            memoryCache.remove(key); // Remove expired or invalid entry
        }
        return null;
    }

    public void remove(String key) {
        memoryCache.remove(key);
    }

    public void clear() {
        memoryCache.evictAll();
    }

    // Helper method to generate cache keys
    public static String generateKey(String prefix, String identifier) {
        return prefix + "_" + identifier;
    }
}

