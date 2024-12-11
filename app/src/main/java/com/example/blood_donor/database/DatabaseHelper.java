package com.example.blood_donor.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BloodDonor.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_EVENTS = "events";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_REGISTRATIONS = "registrations";
    private static final String TABLE_NOTIFICATIONS = "notifications";
    private static final String TABLE_SESSIONS = "sessions";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";

    // Users Table Columns
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_DATE_OF_BIRTH = "date_of_birth";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_BLOOD_TYPE = "blood_type";
    private static final String KEY_GENDER = "gender";

    // Create Users Table
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + "(" +
                    KEY_ID + " TEXT PRIMARY KEY," +
                    KEY_EMAIL + " TEXT UNIQUE NOT NULL," +
                    KEY_PASSWORD + " TEXT NOT NULL," +
                    KEY_FULL_NAME + " TEXT NOT NULL," +
                    KEY_DATE_OF_BIRTH + " INTEGER NOT NULL," +
                    KEY_PHONE_NUMBER + " TEXT," +
                    KEY_USER_TYPE + " TEXT NOT NULL," +
                    KEY_BLOOD_TYPE + " TEXT," +
                    KEY_GENDER + " TEXT," +
                    KEY_CREATED_AT + " INTEGER NOT NULL," +
                    KEY_UPDATED_AT + " INTEGER NOT NULL" +
                    ")";

    // Index for email searches
    private static final String CREATE_EMAIL_INDEX =
            "CREATE INDEX idx_users_email ON " + TABLE_USERS + "(" + KEY_EMAIL + ")";

    private static final String CREATE_TABLE_SESSIONS =
            "CREATE TABLE " + TABLE_SESSIONS + "(" +
                    "token TEXT PRIMARY KEY," +
                    "user_id TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Create existing tables
            db.execSQL(CREATE_TABLE_USERS);

            // Create sessions table
            db.execSQL(
                    CREATE_TABLE_SESSIONS
            );

            // Create indices
            db.execSQL(CREATE_EMAIL_INDEX);
            db.execSQL("CREATE INDEX idx_sessions_user_id ON sessions(user_id)");
        } catch (SQLException e) {
            Log.e("DatabaseHelper", "Error creating database", e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            // In development, we might want to drop and recreate
            // In production, we should migrate data properly
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        } catch (SQLException e) {
            Log.e("DatabaseHelper", "Error upgrading database", e);
            throw e;
        }
    }

    // Helper method to get readable database with error handling
    public SQLiteDatabase getReadableDatabaseWithException() throws AppException {
        try {
            return getReadableDatabase();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Could not open database for reading: " + e.getMessage());
        }
    }

    // Helper method to get writable database with error handling
    public SQLiteDatabase getWritableDatabaseWithException() throws AppException {
        try {
            return getWritableDatabase();
        } catch (SQLiteException e) {
            throw new AppException(ErrorCode.DATABASE_ERROR,
                    "Could not open database for writing: " + e.getMessage());
        }
    }

    // Helper method to check if table exists
    public boolean isTableExists(String tableName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tableName}
            );
            return cursor != null && cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Helper method to get table row count
    public int getTableRowCount(String tableName) throws AppException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabaseWithException();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
