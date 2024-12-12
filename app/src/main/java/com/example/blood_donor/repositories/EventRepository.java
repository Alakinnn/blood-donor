package com.example.blood_donor.repositories;

import static com.example.blood_donor.database.DatabaseHelper.TABLE_EVENTS;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.utils.QueryBuilder;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        String bloodTypesJson = cursor.getString(cursor.getColumnIndexOrThrow("required_blood_types"));
        List<String> bloodTypes = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(bloodTypesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                bloodTypes.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            // Handle error or use empty list
        }

        // Create the DonationEvent object
        return new DonationEvent(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getLong(cursor.getColumnIndexOrThrow("start_time")),
                cursor.getLong(cursor.getColumnIndexOrThrow("end_time")),
                location,
                bloodTypes,
                cursor.getDouble(cursor.getColumnIndexOrThrow("blood_goal")),
                cursor.getString(cursor.getColumnIndexOrThrow("host_id"))
        );
    }

    @Override
    public int countEvents(EventQueryDTO query) throws AppException {
        SQLiteDatabase db = null;
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
            values.put("blood_goal", event.getBloodGoal());
            values.put("current_blood_collected", event.getCurrentBloodCollected());
            values.put("required_blood_types", new JSONArray(event.getRequiredBloodTypes()).toString());
            values.put("host_id", event.getHostId());
            values.put("status", event.getStatus().name());
            values.put("location_id", event.getLocation().getLocationId());
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            long result = db.insert(TABLE_EVENTS, null, values);
            if (result == -1) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save event");
            }

            db.setTransactionSuccessful();
            return Optional.of(event);

        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }
}
