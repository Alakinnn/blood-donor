package com.example.blood_donor.server.repositories;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.location.Location;

import java.util.Optional;

public class LocationRepository implements ILocationRepository {
    private static final String TABLE_LOCATIONS = "locations";
    private final DatabaseHelper dbHelper;

    public LocationRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public Optional<Location> save(Location location) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("id", location.getLocationId());
            values.put("address", location.getAddress());
            values.put("latitude", location.getLatitude());
            values.put("longitude", location.getLongitude());
            values.put("description", location.getDescription());
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            long result = db.insert(TABLE_LOCATIONS, null, values);

            if (result == -1) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save location");
            }

            db.setTransactionSuccessful();
            return Optional.of(location);

        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    @Override
    public Optional<Location> findById(String id) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_LOCATIONS,
                    null,
                    "id = ?",
                    new String[]{id},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursorToLocation(cursor));
            }
            return Optional.empty();

        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean delete(String id) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            int rowsAffected = db.delete(
                    TABLE_LOCATIONS,
                    "id = ?",
                    new String[]{id}
            );

            db.setTransactionSuccessful();
            return rowsAffected > 0;

        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    private Location cursorToLocation(Cursor cursor) {
        return new Location.Builder()
                .locationId(cursor.getString(cursor.getColumnIndexOrThrow("id")))
                .address(cursor.getString(cursor.getColumnIndexOrThrow("address")))
                .coordinates(
                        cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                )
                .description(cursor.getString(cursor.getColumnIndexOrThrow("description")))
                .build();
    }
}
