package com.example.blood_donor.server.repositories;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.utils.QueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository implements IUserRepository {
    private static final String TABLE_USERS = "users";
    private final DatabaseHelper dbHelper;

    public UserRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public Optional<User> createUser(User user) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("id", user.getUserId());
            values.put("email", user.getEmail());
            values.put("password", user.getPassword());
            values.put("full_name", user.getFullName());
            values.put("date_of_birth", user.getDateOfBirth());
            values.put("phone_number", user.getPhoneNumber());
            values.put("user_type", user.getUserType().toString());
            values.put("blood_type", user.getBloodType());
            values.put("gender", user.getGender());
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            long result = db.insert(TABLE_USERS, null, values);

            if (result == -1) {
                Log.e("UserRepository", "Failed to insert user: " + user.getEmail());
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to create user");
            }

            // Verify user was created
            cursor = db.query(
                    TABLE_USERS,
                    null,
                    "email = ?",
                    new String[]{user.getEmail()},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Log.d("UserRepository", "User created successfully: " + user.getEmail());
                db.setTransactionSuccessful();
                return Optional.of(user);
            } else {
                Log.e("UserRepository", "User not found after creation: " + user.getEmail());
                throw new AppException(ErrorCode.DATABASE_ERROR, "User creation verification failed");
            }

        } catch (SQLiteException e) {
            Log.e("UserRepository", "Database error creating user: " + e.getMessage());
            throw new AppException(ErrorCode.DATABASE_ERROR, "Database error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    @Override
    public Optional<User> findByEmail(String email) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_USERS,
                    null,
                    "email = ?",
                    new String[]{email},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursorToUser(cursor));
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
    public Optional<User> findById(String id) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_USERS,
                    null,
                    "id = ?",
                    new String[]{id},
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursorToUser(cursor));
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
    public boolean existsByEmail(String email) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    TABLE_USERS,
                    new String[]{"id"},
                    "email = ?",
                    new String[]{email},
                    null,
                    null,
                    null
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
    public boolean updateUser(User user) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("full_name", user.getFullName());
            values.put("phone_number", user.getPhoneNumber());
            values.put("blood_type", user.getBloodType());
            values.put("gender", user.getGender());
            values.put("updated_at", System.currentTimeMillis());

            int rowsAffected = db.update(
                    TABLE_USERS,
                    values,
                    "id = ?",
                    new String[]{user.getUserId()}
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

    @Override
    public boolean deleteUser(String id) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            int rowsAffected = db.delete(
                    TABLE_USERS,
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

    private User cursorToUser(Cursor cursor) {
        return new User(
                cursor.getString(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("password")),
                cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                cursor.getLong(cursor.getColumnIndexOrThrow("date_of_birth")),
                cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                UserType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("user_type"))),
                cursor.getString(cursor.getColumnIndexOrThrow("blood_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("gender"))
        );
    }

    @Override
    public List<User> findUsersByTimeRange(long startTime, long endTime) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            QueryBuilder queryBuilder = new QueryBuilder()
                    .select("*")
                    .from("users")
                    .where("created_at >= ? AND created_at <= ?",
                            String.valueOf(startTime),
                            String.valueOf(endTime));

            cursor = queryBuilder.execute(db);

            List<User> users = new ArrayList<>();
            while (cursor.moveToNext()) {
                users.add(cursorToUser(cursor));
            }
            return users;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
