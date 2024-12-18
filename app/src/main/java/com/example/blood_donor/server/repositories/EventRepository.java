package com.example.blood_donor.server.repositories;

import static com.example.blood_donor.server.database.DatabaseHelper.TABLE_EVENTS;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.event.BloodTypeRequirement;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.location.Location;
import com.example.blood_donor.server.utils.QueryBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.ArrayList;
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
    public List<DonationEvent> findEvents(EventQueryDTO query) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.select("e.*, l.*")
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

            // Handle distance-based operations if location provided
            if (query.getLatitude() != null && query.getLongitude() != null) {
                handleDistanceOperations(events, query);
            }

            return events;
        } finally {
            if (cursor != null) {
                cursor.close();
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
        // First create the Location object since DonationEvent needs it
        Location location = new Location.Builder()
                .locationId(cursor.getString(cursor.getColumnIndexOrThrow("location_id")))
                .address(cursor.getString(cursor.getColumnIndexOrThrow("address")))
                .coordinates(
                        cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                )
                .description(cursor.getString(cursor.getColumnIndexOrThrow("description")))
                .build();

        // Convert blood types string from DB to List
        Map<String, Double> bloodTypeTargets = new HashMap<>();
        // Parse blood_type_targets from JSON string
        String targetsJson = cursor.getString(cursor.getColumnIndexOrThrow("blood_type_targets"));
        try {
            JSONObject targets = new JSONObject(targetsJson);
            Iterator<String> keys = targets.keys();
            while(keys.hasNext()) {
                String type = keys.next();
                bloodTypeTargets.put(type, targets.getDouble(type));
            }
        } catch (JSONException e) {
            // Handle error
        }

        LocalTime donationStartTime = null;
        LocalTime donationEndTime = null;

        String startTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("donation_start_time"));
        String endTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("donation_end_time"));

        if (startTimeStr != null && endTimeStr != null) {
            donationStartTime = LocalTime.parse(startTimeStr);
            donationEndTime = LocalTime.parse(endTimeStr);
        }


        // Create the DonationEvent object
        DonationEvent event = new DonationEvent(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("start_time")),
                cursor.getLong(cursor.getColumnIndexOrThrow("end_time")),
                location,
                bloodTypeTargets,
                cursor.getString(cursor.getColumnIndexOrThrow("host_id")),
                donationStartTime,
                donationEndTime
        );

        String collectedJson = cursor.getString(cursor.getColumnIndexOrThrow("blood_collected"));
        try {
            JSONObject collected = new JSONObject(collectedJson);
            Iterator<String> keys = collected.keys();
            while(keys.hasNext()) {
                String type = keys.next();
                double amount = collected.getDouble(type);
                event.recordDonation(type, amount);
            }
        } catch (JSONException e) {
            // Handle error
        }

        return event;
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

            QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.select("e.*, l.*")
                    .from("events e")
                    .join("locations l ON e.location_id = l.id")
                    .where("e.id = ?", eventId);

            cursor = queryBuilder.execute(db);

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursorToEvent(cursor));
            }

            return Optional.empty();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
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

            // Convert blood type targets to JSON
            JSONObject bloodTypeTargets = new JSONObject();
            for (Map.Entry<String, BloodTypeRequirement> entry : event.getBloodRequirements().entrySet()) {
                bloodTypeTargets.put(entry.getKey(), entry.getValue().getTargetAmount());
            }
            values.put("blood_type_targets", bloodTypeTargets.toString());

            if (event.getDonationStartTime() != null) {
                values.put("donation_start_time", event.getDonationStartTime().toString());
            }
            if (event.getDonationEndTime() != null) {
                values.put("donation_end_time", event.getDonationEndTime().toString());
            }

            // Add blood_collected as empty JSON object - this was missing before
            JSONObject bloodCollected = new JSONObject();
            for (Map.Entry<String, BloodTypeRequirement> entry : event.getBloodRequirements().entrySet()) {
                bloodCollected.put(entry.getKey(), 0.0); // Initialize with 0 collected for each blood type
            }
            values.put("blood_collected", bloodCollected.toString());

            long result = db.insert(TABLE_EVENTS, null, values);
            if (result == -1) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save event");
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
}
