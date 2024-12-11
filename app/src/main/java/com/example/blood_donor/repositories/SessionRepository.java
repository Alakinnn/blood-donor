package com.example.blood_donor.repositories;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;

import java.util.Optional;

public class SessionRepository implements ISessionRepository {
    private static final String TABLE_SESSIONS = "sessions";
    private final DatabaseHelper dbHelper;

    public SessionRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void saveSession(String token, String userId) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("token", token);
            values.put("user_id", userId);
            values.put("created_at", System.currentTimeMillis());

            long result = db.insert(TABLE_SESSIONS, null, values);

            if (result == -1) {
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save session");
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    @Override
    public Optional<String> getUserIdByToken(String token) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_SESSIONS,
                    new String[]{"user_id"},
                    "token = ?",
                    new String[]{token},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursor.getString(cursor.getColumnIndexOrThrow("user_id")));
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
    public void deleteSession(String token) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(TABLE_SESSIONS, "token = ?", new String[]{token});

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    @Override
    public void deleteAllUserSessions(String userId) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            db.delete(TABLE_SESSIONS, "user_id = ?", new String[]{userId});

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }
}