package com.example.blood_donor.ui.utils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.user.UserType;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DatabaseSeeder {
    private final DatabaseHelper dbHelper;
    private final Random random = new Random();

    // Sydney area coordinates
    private static final double BASE_LAT = 37.3669;
    private static final double BASE_LNG = -122.0847;
    private static final double COORD_SPREAD = 0.05;

    // Test data arrays
    private static final String[] MANAGER_NAMES = {
            "John Smith", "Jane Doe", "Mike Johnson", "Sarah Wilson", "David Brown"
    };

    private static final String[] EVENT_TITLES = {
            "Emergency Blood Drive at Sydney Hospital",
            "University of Sydney Blood Donation",
            "Corporate Blood Drive - CBD",
            "Community Blood Drive - Surry Hills",
            "Weekend Warriors Blood Drive",
            "North Shore Hospital Blood Drive",
            "Tech Industry Blood Drive",
            "Central Station Blood Drive",
            "Westfield Shopping Centre Drive",
            "Bondi Beach Blood Drive"
    };

    private static final String[] EVENT_DESCRIPTIONS = {
            "Critical blood supplies needed! Join us for this emergency blood drive.",
            "Help save lives in your community. Every donation counts!",
            "Be a hero - donate blood and save up to three lives.",
            "Quick and easy donation process with experienced staff.",
            "Join our regular blood drive initiative with free health checkup.",
            "Urgent blood needed for emergency surgeries. Please donate!",
            "Make a difference in someone's life today.",
            "All blood types needed, especially O negative.",
            "First-time donors welcome! Refreshments provided.",
            "Help maintain our community's blood supply."
    };

    private static final String[] NEARBY_ADDRESSES = {
            "615 Cuesta Dr, Mountain View, CA 94040", // Cuesta Park
            "1701 Rock St, Mountain View, CA 94043", // El Camino Hospital
            "2025 Grant Rd, Mountain View, CA 94040", // Grant Park Plaza
            "201 S Rengstorff Ave, Mountain View, CA 94040", // Rengstorff Park
            "1500 Miramonte Ave, Mountain View, CA 94040" // Los Altos High School
    };

    public DatabaseSeeder(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void seedDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            createTestAccounts(db);

            // Create test managers
            String[] managerIds = createTestManagers(db);

            // Create test events
            createTestEvents(db, managerIds);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseSeeder", "Error seeding database", e);
        } finally {
            db.endTransaction();
        }
    }

    private String[] createTestManagers(SQLiteDatabase db) {
        String[] managerIds = new String[MANAGER_NAMES.length];

        for (int i = 0; i < MANAGER_NAMES.length; i++) {
            String managerId = UUID.randomUUID().toString();
            managerIds[i] = managerId;

            ContentValues values = new ContentValues();
            values.put("id", managerId);
            values.put("email", MANAGER_NAMES[i].toLowerCase().replace(" ", ".") + "@example.com");
            values.put("password", "$2a$10$xxxxxxxxxxx"); // Placeholder hashed password
            values.put("full_name", MANAGER_NAMES[i]);
            values.put("date_of_birth", System.currentTimeMillis() - (random.nextLong() % (50L * 365 * 24 * 60 * 60 * 1000))); // Random age between 25-50
            values.put("phone_number", String.format("+61%08d", random.nextInt(100000000)));
            values.put("user_type", UserType.SITE_MANAGER.name());
            values.put("gender", random.nextBoolean() ? "M" : "F");
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            db.insert("users", null, values);
        }

        return managerIds;
    }

    private void createTestEvents(SQLiteDatabase db, String[] managerIds) {
        for (int i = 0; i < 5; i++) {
            // Create location
            String locationId = UUID.randomUUID().toString();
            double lat = BASE_LAT + (random.nextDouble() * COORD_SPREAD * 2 - COORD_SPREAD);
            double lng = BASE_LNG + (random.nextDouble() * COORD_SPREAD * 2 - COORD_SPREAD);

            ContentValues locationValues = new ContentValues();
            locationValues.put("id", locationId);
            locationValues.put("address", NEARBY_ADDRESSES[random.nextInt(NEARBY_ADDRESSES.length)]);
            locationValues.put("latitude", lat);
            locationValues.put("longitude", lng);
            locationValues.put("description", "Floor " + (random.nextInt(5) + 1));
            locationValues.put("created_at", System.currentTimeMillis());
            locationValues.put("updated_at", System.currentTimeMillis());

            db.insert("locations", null, locationValues);

            // Create event
            String eventId = UUID.randomUUID().toString();
            Calendar startTime = Calendar.getInstance();
            startTime.add(Calendar.DAY_OF_MONTH, random.nextInt(30) - 15); // Events from -15 to +15 days
            Calendar endTime = (Calendar) startTime.clone();
            endTime.add(Calendar.HOUR, 8);

            // Default donation hours (9 AM to 5 PM)
            String donationStartTime = "09:00";
            String donationEndTime = "17:00";

            // Create blood type targets
            Map<String, Double> bloodTypeTargets = new HashMap<>();
            bloodTypeTargets.put("A+", 10.0 + random.nextDouble() * 20.0);
            bloodTypeTargets.put("O-", 15.0 + random.nextDouble() * 20.0);
            bloodTypeTargets.put("B+", 8.0 + random.nextDouble() * 15.0);

            // Create collected amounts (random percentage of targets)
            Map<String, Double> bloodCollected = new HashMap<>();
            bloodTypeTargets.forEach((type, target) ->
                    bloodCollected.put(type, target * random.nextDouble()));

            ContentValues eventValues = new ContentValues();
            eventValues.put("id", eventId);
            eventValues.put("title", EVENT_TITLES[random.nextInt(EVENT_TITLES.length)]);
            eventValues.put("description", EVENT_DESCRIPTIONS[random.nextInt(EVENT_DESCRIPTIONS.length)]);
            eventValues.put("start_time", startTime.getTimeInMillis());
            eventValues.put("end_time", endTime.getTimeInMillis());
            eventValues.put("blood_type_targets", new JSONObject(bloodTypeTargets).toString());
            eventValues.put("blood_collected", new JSONObject(bloodCollected).toString());
            eventValues.put("host_id", managerIds[random.nextInt(managerIds.length)]);
            eventValues.put("status", determineEventStatus(startTime.getTimeInMillis(), endTime.getTimeInMillis()));
            eventValues.put("location_id", locationId);
            eventValues.put("donation_start_time", donationStartTime);
            eventValues.put("donation_end_time", donationEndTime);
            eventValues.put("created_at", System.currentTimeMillis());
            eventValues.put("updated_at", System.currentTimeMillis());

            db.insert("events", null, eventValues);
        }
    }

    private String determineEventStatus(long startTime, long endTime) {
        long now = System.currentTimeMillis();
        if (now < startTime) return EventStatus.UPCOMING.name();
        if (now > endTime) return EventStatus.COMPLETED.name();
        return EventStatus.IN_PROGRESS.name();
    }

    private void createTestRegistrations(SQLiteDatabase db) {
        // Create some dummy registrations for events
        // This could be expanded based on your needs
    }

    private void createTestAccounts(SQLiteDatabase db) {
        // Create test donor account
        String testUserId = UUID.randomUUID().toString();
        ContentValues values = new ContentValues();
        values.put("id", testUserId);
        values.put("email", "test@gmail.com");
        values.put("password", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"); // Encrypted "test123@"
        values.put("full_name", "Test Donor");
        values.put("date_of_birth", System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)); // 25 years old
        values.put("blood_type", "O+");
        values.put("user_type", UserType.DONOR.name());
        values.put("gender", "Other");
        values.put("created_at", System.currentTimeMillis());
        values.put("updated_at", System.currentTimeMillis());

        db.insert("users", null, values);
    }
}
