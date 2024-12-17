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
    private static final double BASE_LAT = -33.8688;
    private static final double BASE_LNG = 151.2093;
    private static final double COORD_SPREAD = 0.1;

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

    private static final String[] SYDNEY_ADDRESSES = {
            "123 George Street, Sydney NSW 2000",
            "456 Pitt Street, Sydney NSW 2000",
            "789 Elizabeth Street, Surry Hills NSW 2010",
            "321 Oxford Street, Paddington NSW 2021",
            "654 Crown Street, Surry Hills NSW 2010",
            "987 Military Road, Mosman NSW 2088",
            "147 King Street, Newtown NSW 2042",
            "258 Victoria Street, Darlinghurst NSW 2010",
            "369 Anzac Parade, Kingsford NSW 2032",
            "741 Pacific Highway, Chatswood NSW 2067"
    };

    public DatabaseSeeder(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void seedDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            // Create test managers
            String[] managerIds = createTestManagers(db);

            // Create test events
            createTestEvents(db, managerIds);

            // Create test registrations
            createTestRegistrations(db);

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
        for (int i = 0; i < 30; i++) {
            // Create location
            String locationId = UUID.randomUUID().toString();
            double lat = BASE_LAT + (random.nextDouble() * COORD_SPREAD * 2 - COORD_SPREAD);
            double lng = BASE_LNG + (random.nextDouble() * COORD_SPREAD * 2 - COORD_SPREAD);

            ContentValues locationValues = new ContentValues();
            locationValues.put("id", locationId);
            locationValues.put("address", SYDNEY_ADDRESSES[random.nextInt(SYDNEY_ADDRESSES.length)]);
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
}
