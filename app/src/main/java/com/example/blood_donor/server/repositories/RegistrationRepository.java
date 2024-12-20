package com.example.blood_donor.server.repositories;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.donation.Registration;
import com.example.blood_donor.server.models.donation.RegistrationType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegistrationRepository implements IRegistrationRepository {
    private final DatabaseHelper dbHelper;
    private static final String TABLE_REGISTRATIONS = "registrations";

    public RegistrationRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void register(String userId, String eventId, RegistrationType type) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("registration_id", UUID.randomUUID().toString());
            values.put("user_id", userId);
            values.put("event_id", eventId);
            values.put("type", type.name());
            values.put("registration_time", System.currentTimeMillis());
            values.put("status", "ACTIVE"); // Add status field

            long result = db.insert(TABLE_REGISTRATIONS, null, values);
            if (result == -1) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to register");
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
        }
    }

    @Override
    public boolean isRegistered(String userId, String eventId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM registrations " +
                            "WHERE user_id = ? AND event_id = ? AND status = 'ACTIVE'",
                    new String[]{userId, eventId}
            );

            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
            return false;
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Database error checking registration: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public int getRegistrationCount(String eventId, RegistrationType type) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_REGISTRATIONS,
                    new String[]{"COUNT(*) as count"},
                    "event_id = ? AND type = ? AND status = ?",
                    new String[]{eventId, type.name(), "ACTIVE"},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            }
            return 0;
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void unregister(String userId, String eventId) throws AppException {

    }

    @Override
    public void updateStatus(String registrationId, String status) throws AppException {

    }

    @Override
    public List<Registration> getEventRegistrations(String eventId) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            String query =
                    "SELECT r.registration_id, r.user_id, r.event_id, " +
                            "r.type, r.registration_time, r.status " +
                            "FROM " + DatabaseHelper.TABLE_REGISTRATIONS + " r " +
                            "WHERE r.event_id = ? AND r.status = 'ACTIVE' " +
                            "ORDER BY r.registration_time ASC";

            cursor = db.rawQuery(query, new String[]{eventId});

            List<Registration> registrations = new ArrayList<>();
            while (cursor.moveToNext()) {
                Registration registration = new Registration(
                        cursor.getString(cursor.getColumnIndexOrThrow("registration_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("event_id")),
                        RegistrationType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type")))
                );
                registrations.add(registration);
            }

            return registrations;
        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Error getting event registrations: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}