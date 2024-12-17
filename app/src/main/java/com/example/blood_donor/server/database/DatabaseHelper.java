package com.example.blood_donor.server.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.errors.ErrorCode;

import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "BloodDonor.db";
    private static final int DATABASE_VERSION = 1;
    private static volatile DatabaseHelper instance;
    private final AtomicInteger mOpenCounter = new AtomicInteger();
    private SQLiteDatabase mDatabase;
    private final Object dbLock = new Object();

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
                    "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
                    "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (event_id) REFERENCES " + TABLE_EVENTS + "(id) ON DELETE CASCADE" +
                    ")";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized SQLiteDatabase openDatabase() throws AppException {
        try {
            // Wait for up to 5 seconds to get a connection
            long startTime = System.currentTimeMillis();
            while (mOpenCounter.get() > 0 && System.currentTimeMillis() - startTime < 5000) {
                Thread.sleep(100);
            }

            if (mOpenCounter.incrementAndGet() == 1) {
                synchronized (dbLock) {
                    if (mDatabase == null || !mDatabase.isOpen()) {
                        mDatabase = getWritableDatabase();
                    }
                }
            }
            return mDatabase;
        } catch (Exception e) {
            mOpenCounter.decrementAndGet();
            throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to open database: " + e.getMessage());
        }
    }

    public synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            synchronized (dbLock) {
                if (mDatabase != null && mDatabase.isOpen()) {
                    mDatabase.close();
                    mDatabase = null;
                }
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();

            // Create tables
            db.execSQL(CREATE_TABLE_USERS);
            db.execSQL(CREATE_TABLE_LOCATIONS);
            db.execSQL(CREATE_TABLE_EVENTS);
            db.execSQL(CREATE_TABLE_SESSIONS);
            db.execSQL(CREATE_TABLE_REGISTRATIONS);

            // Create indexes
            createIndexes(db);

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error creating database", e);
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        // Users indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");

        // Sessions indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id)");

        // Events indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_host ON events(host_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_location ON events(location_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_status ON events(status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_time ON events(start_time, end_time)");

        // Locations indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_locations_coords ON locations(latitude, longitude)");

        // Registrations indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_registrations_event ON registrations(event_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_registrations_user ON registrations(user_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_registrations_status ON registrations(status)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // In production, implement proper migration logic
        try {
            db.beginTransaction();

            // Drop all tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTRATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

            // Recreate tables
            onCreate(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public <T> T executeTransaction(DatabaseOperation<T> operation) throws AppException {
        SQLiteDatabase db = null;
        try {
            db = openDatabase();
            db.beginTransaction();
            T result = operation.execute(db);
            db.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "Transaction failed: " + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
                closeDatabase();
            }
        }
    }

    public interface DatabaseOperation<T> {
        T execute(SQLiteDatabase database) throws Exception;
    }
}