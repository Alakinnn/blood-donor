package com.example.blood_donor.repositories;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.donation.RegistrationType;
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
            cursor = db.query(
                    TABLE_REGISTRATIONS,
                    new String[]{"registration_id"},
                    "user_id = ? AND event_id = ? AND status = ?",
                    new String[]{userId, eventId, "ACTIVE"},
                    null, null, null
            );
            return cursor != null && cursor.getCount() > 0;
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
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
}