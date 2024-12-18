package com.example.blood_donor.server.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BloodDonor.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String TABLE_REGISTRATIONS = "registrations";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_SESSIONS = "sessions";

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

    private static final String CREATE_TABLE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + "(" +
                    "id TEXT PRIMARY KEY," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "start_time INTEGER NOT NULL," +
                    "end_time INTEGER NOT NULL," +
                    "blood_type_targets TEXT NOT NULL," + // JSON object of blood type targets
                    "blood_collected TEXT NOT NULL," + // JSON object of collected amounts
                    "host_id TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "location_id TEXT NOT NULL," +
                    "donation_start_time TEXT," +
                    "donation_end_time TEXT," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL," +
                    "FOREIGN KEY (location_id) REFERENCES " + TABLE_LOCATIONS + "(id)," +
                    "FOREIGN KEY (host_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";

    private static final String CREATE_TABLE_LOCATIONS =
            "CREATE TABLE " + TABLE_LOCATIONS + "(" +
                    "id TEXT PRIMARY KEY," +
                    "address TEXT NOT NULL," +
                    "latitude REAL NOT NULL," +
                    "longitude REAL NOT NULL," +
                    "description TEXT," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL," +
                    "CHECK (latitude >= -90 AND latitude <= 90 AND " +
                    "longitude >= -180 AND longitude <= 180)" +
                    ")";
    private static final String CREATE_TABLE_SESSIONS =
            "CREATE TABLE " + TABLE_SESSIONS + "(" +
                    "token TEXT PRIMARY KEY," +
                    "user_id TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(id)" +
                    ")";

    private static final String CREATE_TABLE_REGISTRATIONS =
            "CREATE TABLE " + TABLE_REGISTRATIONS + "(" +
                    "registration_id TEXT PRIMARY KEY," +
                    "user_id TEXT NOT NULL," +
                    "event_id TEXT NOT NULL," +
                    "type TEXT NOT NULL," +  // DONOR or VOLUNTEER
                    "registration_time INTEGER NOT NULL," +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE'," + // Add status field with default
                    "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (event_id) REFERENCES " + TABLE_EVENTS + "(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_LOCATION_INDEXES =
            "CREATE INDEX IF NOT EXISTS idx_location_coordinates ON " +
                    "locations(latitude, longitude)";

    private static final String CREATE_LOCATION_TRIGGERS =
            "CREATE TRIGGER IF NOT EXISTS validate_coordinates " +
                    "BEFORE INSERT ON locations " +
                    "BEGIN " +
                    "    SELECT CASE " +
                    "        WHEN NEW.latitude < -90 OR NEW.latitude > 90 OR " +
                    "             NEW.longitude < -180 OR NEW.longitude > 180 " +
                    "        THEN RAISE(ABORT, 'Invalid coordinates') " +
                    "    END; " +
                    "END;";

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
            db.execSQL(CREATE_TABLE_LOCATIONS);
            db.execSQL(CREATE_TABLE_EVENTS);
            // Add in onCreate():
            db.execSQL(CREATE_TABLE_REGISTRATIONS);
            // Create indexes for Users
            db.execSQL("CREATE INDEX idx_users_email ON users(email)");

            // Create indexes for Sessions
            db.execSQL("CREATE INDEX idx_sessions_user_id ON sessions(user_id)");

            // Create indexes for Events
            db.execSQL("CREATE INDEX idx_events_start_time ON events(start_time)");
            db.execSQL("CREATE INDEX idx_events_status ON events(status)");
            db.execSQL("CREATE INDEX idx_events_location ON events(location_id)");
            db.execSQL("CREATE INDEX idx_events_host ON events(host_id)");

            // Create indexes for Registrations
            db.execSQL("CREATE INDEX idx_registrations_user_event ON registrations(user_id, event_id)");
            db.execSQL("CREATE INDEX idx_registrations_status ON registrations(status)");
            db.execSQL("CREATE INDEX idx_registrations_type_status ON registrations(type, status)");

            // Create indexes for Locations
            db.execSQL("CREATE INDEX idx_locations_coords ON locations(latitude, longitude)");
            db.execSQL(CREATE_LOCATION_INDEXES);
            db.execSQL(CREATE_LOCATION_TRIGGERS);
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

    private static final String LOCATION_TABLE_CONSTRAINTS =
            "CHECK (latitude >= -90 AND latitude <= 90 AND " +
                    "longitude >= -180 AND longitude <= 180)";
}
