package com.example.blood_donor.server.repositories;

import static com.example.blood_donor.server.database.DatabaseHelper.TABLE_EVENTS;
import static com.example.blood_donor.server.database.DatabaseHelper.TABLE_LOCATIONS;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.events.UpdateEventDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.event.BloodTypeRequirement;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.location.Location;
import com.example.blood_donor.server.utils.QueryBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventRepository implements IEventRepository {
    private final DatabaseHelper dbHelper;
    private static final double EARTH_RADIUS = 6371; // kilometers

    public EventRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public List<DonationEvent> findVisibleEvents(EventQueryDTO query) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.select("e.id AS event_id, e.title, e.description AS event_description, " +
                            "e.start_time, e.end_time, e.blood_type_targets, " +
                            "e.blood_collected, e.host_id, e.status, " +
                            "e.donation_start_time, e.donation_end_time, " +
                            "l.id AS location_id, l.address, l.latitude, l.longitude")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id");

            List<String> conditions = new ArrayList<>();
            List<String> args = new ArrayList<>();

            // Base condition
            conditions.add("e.status != ?");
            args.add("CANCELLED");

            // Title-only search term condition (changed from previous implementation)
            if (query.getSearchTerm() != null && !query.getSearchTerm().trim().isEmpty()) {
                String searchTerm = "%" + query.getSearchTerm().trim() + "%";
                conditions.add("e.title LIKE ?");
                args.add(searchTerm);
            }

            // Blood type filter - Modified to require ALL selected blood types
            if (query.getBloodTypes() != null && !query.getBloodTypes().isEmpty()) {
                List<String> bloodTypeConditions = new ArrayList<>();
                for (String bloodType : query.getBloodTypes()) {
                    bloodTypeConditions.add("e.blood_type_targets LIKE ?");
                    args.add("%" + bloodType + "%");
                }
                // Changed from OR to AND to require all blood types
                conditions.add("(" + String.join(" AND ", bloodTypeConditions) + ")");
            }

            // Combine all conditions
            if (!conditions.isEmpty()) {
                queryBuilder.where(String.join(" AND ", conditions),
                        args.toArray(new String[0]));
            }

            // Add sorting
            if ("date".equals(query.getSortBy())) {
                queryBuilder.orderBy("e.start_time " + query.getSortOrder());
            }

            // Add pagination
            queryBuilder.limit(query.getPageSize())
                    .offset((query.getPage() - 1) * query.getPageSize());

            cursor = queryBuilder.execute(db);

            List<DonationEvent> events = new ArrayList<>();
            while (cursor.moveToNext()) {
                events.add(cursorToEvent(cursor));
            }

            // Handle distance calculations if location provided
            if (query.getLatitude() != null && query.getLongitude() != null) {
                for (DonationEvent event : events) {
                    double distance = calculateDistance(
                            query.getLatitude(), query.getLongitude(),
                            event.getLocation().getLatitude(),
                            event.getLocation().getLongitude()
                    );
                    event.setDistance(distance);
                }

                // Sort by distance if requested
                if ("distance".equals(query.getSortBy())) {
                    Collections.sort(events, (e1, e2) -> {
                        int result = Double.compare(e1.getDistance(), e2.getDistance());
                        return "desc".equalsIgnoreCase(query.getSortOrder()) ? -result : result;
                    });
                }
            }

            return events;

        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public List<DonationEvent> findAllEvents(EventQueryDTO query) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.select("e.id AS event_id, e.title, e.description AS event_description, " +
                            "e.start_time, e.end_time, e.blood_type_targets, " +
                            "e.blood_collected, e.host_id, e.status, " +
                            "e.donation_start_time, e.donation_end_time, " +
                            "l.id AS location_id, l.address, l.latitude, l.longitude, " +
                            "l.description AS location_description")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id");

            List<String> conditions = new ArrayList<>();
            List<String> args = new ArrayList<>();

            // Add search conditions
            if (query.getSearchTerm() != null && !query.getSearchTerm().isEmpty()) {
                conditions.add("e.title LIKE ?");
                args.add("%" + query.getSearchTerm() + "%");
            }

            // Add blood type filter
            if (query.getBloodTypes() != null && !query.getBloodTypes().isEmpty()) {
                List<String> bloodTypeConditions = new ArrayList<>();
                for (String bloodType : query.getBloodTypes()) {
                    bloodTypeConditions.add("e.blood_type_targets LIKE ?");
                    args.add("%" + bloodType + "%");
                }
                conditions.add("(" + String.join(" OR ", bloodTypeConditions) + ")");
            }

            // Add date filters
            if (query.getStartDateFilter() != null) {
                conditions.add("e.start_time >= ?");
                args.add(String.valueOf(query.getStartDateFilter()));
            }
            if (query.getEndDateFilter() != null) {
                conditions.add("e.end_time <= ?");
                args.add(String.valueOf(query.getEndDateFilter()));
            }

            // Combine conditions
            if (!conditions.isEmpty()) {
                queryBuilder.where(String.join(" AND ", conditions),
                        args.toArray(new String[0]));
            }

            // Add sorting
            if ("date".equals(query.getSortBy())) {
                queryBuilder.orderBy("e.start_time " + query.getSortOrder());
            }

            // Add pagination
            queryBuilder.limit(query.getPageSize())
                    .offset((query.getPage() - 1) * query.getPageSize());

            cursor = queryBuilder.execute(db);

            List<DonationEvent> events = new ArrayList<>();
            while (cursor.moveToNext()) {
                events.add(cursorToEvent(cursor));
            }

            // Handle distance calculations if location provided
            if (query.getLatitude() != null && query.getLongitude() != null) {
                for (DonationEvent event : events) {
                    double distance = calculateDistance(
                            query.getLatitude(), query.getLongitude(),
                            event.getLocation().getLatitude(),
                            event.getLocation().getLongitude()
                    );
                    event.setDistance(distance);
                }

                if ("distance".equals(query.getSortBy())) {
                    Collections.sort(events, (e1, e2) -> {
                        int result = Double.compare(e1.getDistance(), e2.getDistance());
                        return "desc".equalsIgnoreCase(query.getSortOrder()) ? -result : result;
                    });
                }
            }

            return events;
        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void handleDistanceCalculations(List<DonationEvent> events, EventQueryDTO query) {
        if (query.getLatitude() != null && query.getLongitude() != null) {
            for (DonationEvent event : events) {
                double distance = calculateDistance(
                        query.getLatitude(), query.getLongitude(),
                        event.getLocation().getLatitude(),
                        event.getLocation().getLongitude()
                );
                event.setDistance(distance);
            }

            // Sort by distance if requested
            if ("distance".equals(query.getSortBy())) {
                Collections.sort(events, (e1, e2) -> {
                    int result = Double.compare(e1.getDistance(), e2.getDistance());
                    return "desc".equalsIgnoreCase(query.getSortOrder()) ? -result : result;
                });
            }
        }
    }

    private void handleDistanceOperations(List<DonationEvent> events,
                                          EventQueryDTO query) {
        // Calculate distances
        events.forEach(event -> {
            double distance = calculateDistance(
                    query.getLatitude(), query.getLongitude(),
                    event.getLocation().getLatitude(),
                    event.getLocation().getLongitude()
            );
            event.setDistance(distance);
        });

        // Sort by distance if requested
        if ("distance".equals(query.getSortBy())) {
            Comparator<DonationEvent> distanceComparator =
                    Comparator.comparingDouble(DonationEvent::getDistance);

            if ("desc".equalsIgnoreCase(query.getSortOrder())) {
                distanceComparator = distanceComparator.reversed();
            }

            events.sort(distanceComparator);
        }
    }

    protected double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * c;
    }

    private DonationEvent cursorToEvent(Cursor cursor) {
        try {
            String eventId = cursor.getString(cursor.getColumnIndexOrThrow("event_id"));

            // Get the JSON strings
            String bloodTypeTargetsJson = cursor.getString(cursor.getColumnIndexOrThrow("blood_type_targets"));
            String bloodCollectedJson = cursor.getString(cursor.getColumnIndexOrThrow("blood_collected"));

            // Parse blood type targets into a map
            Map<String, Double> bloodTypeTargets = new HashMap<>();
            try {
                JSONObject targets = new JSONObject(bloodTypeTargetsJson);
                Iterator<String> keys = targets.keys();
                while (keys.hasNext()) {
                    String bloodType = keys.next();
                    try {
                        double targetAmount = targets.getDouble(bloodType);
                        bloodTypeTargets.put(bloodType, targetAmount);
                    } catch (JSONException e) {
                        bloodTypeTargets.put(bloodType, 0.0); // Default to 0 if parsing fails
                    }
                }
            } catch (JSONException e) {
                Log.e("EventRepository", "Error parsing blood type targets JSON", e);
            }

            // Create location
            Location location = new Location.Builder()
                    .locationId(cursor.getString(cursor.getColumnIndexOrThrow("location_id")))
                    .address(cursor.getString(cursor.getColumnIndexOrThrow("address")))
                    .coordinates(
                            cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                    )
                    .build();

            // Parse donation times
            String donationStartTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("donation_start_time"));
            String donationEndTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("donation_end_time"));

            LocalTime donationStartTime = null;
            LocalTime donationEndTime = null;

            if (donationStartTimeStr != null) {
                donationStartTime = LocalTime.parse(donationStartTimeStr);
            }
            if (donationEndTimeStr != null) {
                donationEndTime = LocalTime.parse(donationEndTimeStr);
            }

            // Create event
            DonationEvent event = new DonationEvent(
                    eventId,
                    cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("event_description")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("start_time")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("end_time")),
                    location,
                    bloodTypeTargets,
                    cursor.getString(cursor.getColumnIndexOrThrow("host_id")),
                    donationStartTime,
                    donationEndTime
            );

            // Set collected amounts
            try {
                JSONObject collected = new JSONObject(bloodCollectedJson);
                Iterator<String> keys = collected.keys();
                while (keys.hasNext()) {
                    String bloodType = keys.next();
                    try {
                        double collectedAmount = collected.getDouble(bloodType);
                        event.recordDonation(bloodType, collectedAmount);
                    } catch (JSONException e) {
                        Log.e("EventRepository", "Error parsing collected amount for blood type: " + bloodType, e);
                    }
                }
            } catch (JSONException e) {
                Log.e("EventRepository", "Error parsing blood collected JSON", e);
            }
            String statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            event.setStatus(EventStatus.valueOf(statusStr));

            return event;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing event data", e);
        }
    }

    private Map<String, Double> parseBloodTypeTargets(String bloodTypeTargetsJson) {
        Map<String, Double> bloodTypeTargets = new HashMap<>();
        if (bloodTypeTargetsJson != null) {
            try {
                JSONObject targets = new JSONObject(bloodTypeTargetsJson);
                Iterator<String> keys = targets.keys();
                while (keys.hasNext()) {
                    String type = keys.next();
                    bloodTypeTargets.put(type, targets.getDouble(type));
                }
            } catch (JSONException e) {
                throw new RuntimeException("Error parsing blood type targets", e);
            }
        }
        return bloodTypeTargets;
    }

    @Override
    public int countEvents(EventQueryDTO query) throws AppException {
        SQLiteDatabase db;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.select("COUNT(*) as count")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id")
                    .where("e.status != ?", "CANCELLED");

            // Add search conditions
            if (query.getSearchTerm() != null && !query.getSearchTerm().isEmpty()) {
                queryBuilder.and("e.title LIKE ?", "%" + query.getSearchTerm() + "%");
            }

            // Add blood type filter
            if (query.getBloodTypes() != null && !query.getBloodTypes().isEmpty()) {
                String bloodTypeCondition = query.getBloodTypes().stream()
                        .map(type -> "required_blood_types LIKE ?")
                        .collect(Collectors.joining(" OR "));
                queryBuilder.and("(" + bloodTypeCondition + ")",
                        query.getBloodTypes().stream()
                                .map(type -> "%" + type + "%")
                                .toArray(String[]::new));
            }

            cursor = queryBuilder.execute(db);

            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            }
            return 0;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public Optional<DonationEvent> findById(String eventId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {

            db = dbHelper.getReadableDatabase();

            // First verify the event exists
            Cursor checkCursor = db.query(
                    "events",
                    new String[]{"id"},
                    "id = ?",
                    new String[]{eventId},
                    null, null, null
            );

            if (checkCursor != null && checkCursor.getCount() > 0) {
                Log.d("EventRepository", "Event exists in events table");
            } else {
                Log.d("EventRepository", "Event does not exist in events table");
            }
            checkCursor.close();

            // Now do the full query with join
            String query = "SELECT e.id AS event_id, e.title, e.description AS event_description, " +
                    "e.start_time, e.end_time, e.blood_type_targets, " +
                    "e.blood_collected, e.host_id, e.status, " +
                    "e.donation_start_time, e.donation_end_time, " +
                    "l.id AS location_id, l.address, l.latitude, l.longitude, l.description AS location_description " +
                    "FROM events e " +
                    "JOIN locations l ON e.location_id = l.id " +
                    "WHERE e.id = ?";

            cursor = db.rawQuery(query, new String[]{eventId});

            if (cursor != null && cursor.moveToFirst()) {
                Log.d("EventRepository", "Event found with location data");
                // Verify we have all required columns
                String[] columns = cursor.getColumnNames();

                return Optional.of(cursorToEvent(cursor));
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public Optional<DonationEvent> save(DonationEvent event) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("id", event.getEventId());
            values.put("title", event.getTitle());
            values.put("description", event.getDescription());
            values.put("start_time", event.getStartTime());
            values.put("end_time", event.getEndTime());
            values.put("host_id", event.getHostId());
            values.put("status", event.getStatus().name());
            values.put("location_id", event.getLocation().getLocationId());
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            // Convert blood type targets to JSON with numeric values
            JSONObject bloodTypeTargets = new JSONObject();
            for (Map.Entry<String, BloodTypeRequirement> entry : event.getBloodRequirements().entrySet()) {
                BloodTypeRequirement req = entry.getValue();
                // Store the target amount as a number
                bloodTypeTargets.put(entry.getKey(), req.getTargetAmount());
            }
            values.put("blood_type_targets", bloodTypeTargets.toString());

            // Convert blood collected amounts to JSON with numeric values
            JSONObject bloodCollected = new JSONObject();
            for (Map.Entry<String, BloodTypeRequirement> entry : event.getBloodRequirements().entrySet()) {
                BloodTypeRequirement req = entry.getValue();
                // Store the collected amount as a number
                bloodCollected.put(entry.getKey(), req.getCollectedAmount());
            }
            values.put("blood_collected", bloodCollected.toString());

            if (event.getDonationStartTime() != null) {
                values.put("donation_start_time", event.getDonationStartTime().toString());
            }
            if (event.getDonationEndTime() != null) {
                values.put("donation_end_time", event.getDonationEndTime().toString());
            }

            // Try update first
            int rowsAffected = db.update(
                    TABLE_EVENTS,
                    values,
                    "id = ?",
                    new String[]{event.getEventId()}
            );

            // If no rows were updated, try insert
            if (rowsAffected == 0) {
                long result = db.insert(TABLE_EVENTS, null, values);
                if (result == -1) {
                    throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save event");
                }
            }

            db.setTransactionSuccessful();
            return Optional.of(event);

        } catch (SQLiteException | JSONException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    @Override
    public List<DonationEvent> findEventsBetween(long startTime, long endTime) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder()
                    .select("e.*, l.*")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id")
                    .where("e.start_time >= ? AND e.end_time <= ?",
                            String.valueOf(startTime),
                            String.valueOf(endTime));

            cursor = queryBuilder.execute(db);

            List<DonationEvent> events = new ArrayList<>();
            while (cursor.moveToNext()) {
                events.add(cursorToEvent(cursor));
            }
            return events;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public List<DonationEvent> findEventsByHostId(String hostId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder()
                    .select("e.*, l.*")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id")
                    .where("e.host_id = ?", hostId);

            cursor = queryBuilder.execute(db);

            List<DonationEvent> events = new ArrayList<>();
            while (cursor.moveToNext()) {
                events.add(cursorToEvent(cursor));
            }
            return events;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public List<EventSummaryDTO> findJoinedEvents(String userId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            // Query with joins and aliases
            String query =
                    "SELECT e.id AS event_id, e.title, e.description, " +
                            "e.start_time, e.end_time, e.blood_type_targets, " +
                            "e.blood_collected, e.status, e.donation_start_time, " +
                            "e.donation_end_time, " +
                            "l.address, l.latitude, l.longitude, " +
                            "r.type AS registration_type " +
                            "FROM " + DatabaseHelper.TABLE_EVENTS + " e " +
                            "JOIN " + TABLE_LOCATIONS + " l ON e.location_id = l.id " +
                            "JOIN " + DatabaseHelper.TABLE_REGISTRATIONS + " r ON e.id = r.event_id " +
                            "WHERE r.user_id = ? AND r.status = 'ACTIVE' " +
                            "ORDER BY e.start_time DESC";

            cursor = db.rawQuery(query, new String[]{userId});
            return convertToEventSummaries(cursor);
        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Error finding joined events: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public List<EventSummaryDTO> findManagedEvents(String userId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            // Query with joins and aliases
            String query =
                    "SELECT e.id AS event_id, e.title, e.description, " +
                            "e.start_time, e.end_time, e.blood_type_targets, " +
                            "e.blood_collected, e.status, e.donation_start_time, " +
                            "e.donation_end_time, " +
                            "l.address, l.latitude, l.longitude, " +
                            "(SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REGISTRATIONS +
                            " r WHERE r.event_id = e.id AND r.type = 'DONOR' AND r.status = 'ACTIVE') AS donor_count, " +
                            "(SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REGISTRATIONS +
                            " r WHERE r.event_id = e.id AND r.type = 'VOLUNTEER' AND r.status = 'ACTIVE') AS volunteer_count " +
                            "FROM " + DatabaseHelper.TABLE_EVENTS + " e " +
                            "JOIN " + TABLE_LOCATIONS + " l ON e.location_id = l.id " +
                            "WHERE e.host_id = ? " +
                            "ORDER BY e.start_time DESC";

            cursor = db.rawQuery(query, new String[]{userId});
            return convertToEventSummaries(cursor);
        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Error finding managed events: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private List<EventSummaryDTO> convertToEventSummaries(Cursor cursor) {
        List<EventSummaryDTO> summaries = new ArrayList<>();
        while (cursor.moveToNext()) {
            EventSummaryDTO summary = new EventSummaryDTO();
            summary.setEventId(cursor.getString(cursor.getColumnIndexOrThrow("event_id")));
            summary.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            summary.setStartTime(cursor.getLong(cursor.getColumnIndexOrThrow("start_time")));
            summary.setEndTime(cursor.getLong(cursor.getColumnIndexOrThrow("end_time")));
            summary.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            summary.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
            summary.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));

            // Parse blood type targets from JSON
            String bloodTypeTargetsJson = cursor.getString(
                    cursor.getColumnIndexOrThrow("blood_type_targets"));
            String bloodCollectedJson = cursor.getString(
                    cursor.getColumnIndexOrThrow("blood_collected"));

            try {
                JSONObject targets = new JSONObject(bloodTypeTargetsJson);
                JSONObject collected = new JSONObject(bloodCollectedJson);

                double totalGoal = 0;
                double totalCollected = 0;
                List<String> requiredTypes = new ArrayList<>();

                Iterator<String> keys = targets.keys();
                while (keys.hasNext()) {
                    String bloodType = keys.next();
                    requiredTypes.add(bloodType);
                    totalGoal += targets.getDouble(bloodType);
                    totalCollected += collected.optDouble(bloodType, 0);
                }

                summary.setBloodGoal(totalGoal);
                summary.setCurrentBloodCollected(totalCollected);
                summary.setRequiredBloodTypes(requiredTypes);
            } catch (JSONException e) {
                Log.e("EventRepository", "Error parsing blood type data", e);
            }

            // Parse donation times
            String startTimeStr = cursor.getString(
                    cursor.getColumnIndexOrThrow("donation_start_time"));
            String endTimeStr = cursor.getString(
                    cursor.getColumnIndexOrThrow("donation_end_time"));

            if (startTimeStr != null && endTimeStr != null) {
                summary.setDonationStartTime(LocalTime.parse(startTimeStr));
                summary.setDonationEndTime(LocalTime.parse(endTimeStr));
            }
            String statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            summary.setStatus(EventStatus.valueOf(statusStr));

            summaries.add(summary);
        }
        return summaries;
    }

    @Override
    public Optional<DonationEvent> updateEvent(String eventId, UpdateEventDTO updateDto) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // First verify the event exists and get current state with proper aliasing
            String selectQuery =
                    "SELECT e.blood_collected " +
                            "FROM " + TABLE_EVENTS + " e " +
                            "WHERE e.id = ?";

            cursor = db.rawQuery(selectQuery, new String[]{eventId});

            JSONObject currentCollected = new JSONObject();
            if (cursor.moveToFirst()) {
                String collectedJson = cursor.getString(cursor.getColumnIndexOrThrow("blood_collected"));
                if (collectedJson != null) {
                    currentCollected = new JSONObject(collectedJson);
                }
            }
            cursor.close();

            // Update location if address changed
            ContentValues locationValues = new ContentValues();
            locationValues.put("address", updateDto.getAddress());
            locationValues.put("latitude", updateDto.getLatitude());
            locationValues.put("longitude", updateDto.getLongitude());
            locationValues.put("updated_at", System.currentTimeMillis());

            // Get current blood collection and update appropriately
            JSONObject newCollected = new JSONObject();
            JSONObject bloodTypeTargets = new JSONObject(updateDto.getBloodTypeTargets());

            // For each blood type in the new targets
            Iterator<String> keys = bloodTypeTargets.keys();

            // For each blood type in the new targets
            while (keys.hasNext()) {
                String bloodType = keys.next();
                if (currentCollected.has(bloodType)) {
                    // Keep existing collected amount
                    newCollected.put(bloodType, currentCollected.get(bloodType));
                } else {
                    // Only initialize new blood types with 0
                    newCollected.put(bloodType, 0.0);
                }
            }

            // Update event details
            ContentValues eventValues = new ContentValues();
            eventValues.put("title", updateDto.getTitle());
            eventValues.put("description", updateDto.getDescription());
            eventValues.put("start_time", updateDto.getStartTime());
            eventValues.put("end_time", updateDto.getEndTime());
            eventValues.put("blood_type_targets", bloodTypeTargets.toString());
            eventValues.put("blood_collected", newCollected.toString());
            eventValues.put("donation_start_time", updateDto.getDonationStartTime().toString());
            eventValues.put("donation_end_time", updateDto.getDonationEndTime().toString());
            eventValues.put("updated_at", System.currentTimeMillis());

            int rowsUpdated = db.update(TABLE_EVENTS, eventValues, "id = ?", new String[]{eventId});
            if (rowsUpdated == 0) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to update event");
            }

            db.setTransactionSuccessful();

            // Get updated event with proper table aliasing
            String getUpdatedEventQuery =
                    "SELECT e.*, l.id AS location_id, l.address, l.latitude, l.longitude, l.description AS location_description " +
                            "FROM " + TABLE_EVENTS + " e " +
                            "JOIN " + TABLE_LOCATIONS + " l ON e.location_id = l.id " +
                            "WHERE e.id = ?";

            return findById(eventId);

        } catch (JSONException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Error processing blood type data: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) {
                db.endTransaction();
            }
        }
    }
}
