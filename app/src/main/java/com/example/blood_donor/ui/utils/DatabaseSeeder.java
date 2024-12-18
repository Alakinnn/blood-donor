package com.example.blood_donor.ui.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.services.AuthService;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DatabaseSeeder {
    private final DatabaseHelper dbHelper;
    private final AuthService authService;

    // Test account credentials
    private static final String ADMIN_EMAIL = "admin@blooddonor.com";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private static final String[][] DONOR_ACCOUNTS = {
            // email, password, name, bloodType
            {"donor1@test.com", "Donor123!", "John Smith", "A+"},
            {"donor2@test.com", "Donor123!", "Sarah Johnson", "O-"},
            {"donor3@test.com", "Donor123!", "Michael Brown", "B+"},
            {"donor4@test.com", "Donor123!", "Emily Davis", "AB+"},
            {"donor5@test.com", "Donor123!", "David Wilson", "O+"}
    };

    private static final String[][] MANAGER_ACCOUNTS = {
            // email, password, name, phone
            {"manager1@test.com", "Manager123!", "Robert Taylor", "+1234567890"},
            {"manager2@test.com", "Manager123!", "Lisa Anderson", "+1234567891"},
            {"manager3@test.com", "Manager123!", "James Martinez", "+1234567892"},
            {"manager4@test.com", "Manager123!", "Patricia Lee", "+1234567893"},
            {"manager5@test.com", "Manager123!", "William Clark", "+1234567894"}
    };

    private static final String[][] EVENT_DATA = {
            // title, description, address, bloodTypes (comma-separated), totalGoal
            {
                    "Emergency Blood Drive - Mountain View Hospital",
                    "Critical blood supplies needed for emergency surgeries. All blood types welcome, especially O-negative. Free health screening for all donors.",
                    "2500 Grant Road, Mountain View, CA 94040",
                    "O-,A+,B+",
                    "100"
            },
            {
                    "Community Blood Drive - Cuesta Park",
                    "Join our weekend community blood drive. Each donation can save up to three lives! Refreshments provided for all donors.",
                    "615 Cuesta Dr, Mountain View, CA 94040",
                    "ALL",
                    "150"
            },
            {
                    "Tech Companies United Blood Drive",
                    "Silicon Valley's biggest blood drive event. Special focus on rare blood types. Free tech swag for all donors!",
                    "1600 Amphitheatre Parkway, Mountain View, CA 94043",
                    "AB-,B-,O+",
                    "200"
            },
            {
                    "Student Blood Drive - Los Altos High",
                    "Support your community by donating blood. First-time donors welcome! Student ID required for special recognition.",
                    "201 Almond Ave, Los Altos, CA 94022",
                    "A+,O+,B+",
                    "80"
            },
            {
                    "Senior Center Blood Drive",
                    "Help us meet our community's blood supply needs. Special assistance available for elderly donors. Morning appointments preferred.",
                    "97 Hillview Ave, Los Altos, CA 94022",
                    "A+,O-,AB+",
                    "120"
            }
    };

    // Mountain View area coordinates
    private static final double BASE_LAT = 37.3861;
    private static final double BASE_LNG = -122.0839;
    private static final double COORD_SPREAD = 0.02;

    public DatabaseSeeder(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.authService = new AuthService();
    }

    public void seedDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            Log.d("DatabaseSeeder", "Starting database seeding");
            db.beginTransaction();

            // Create admin account
            String adminId = createAdminAccount(db);
            Log.d("DatabaseSeeder", "Created admin account: " + adminId);

            // Create donor accounts
            String[] donorIds = createDonorAccounts(db);
            Log.d("DatabaseSeeder", "Created " + donorIds.length + " donor accounts");

            // Create manager accounts
            String[] managerIds = createManagerAccounts(db);
            Log.d("DatabaseSeeder", "Created " + managerIds.length + " manager accounts");

            // Create events
            createEvents(db, managerIds);

            db.setTransactionSuccessful();
            Log.d("DatabaseSeeder", "Database seeding completed successfully");
        } catch (Exception e) {
            Log.e("DatabaseSeeder", "Error seeding database", e);
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    private String createAdminAccount(SQLiteDatabase db) throws Exception {
        String adminId = UUID.randomUUID().toString();
        ContentValues values = new ContentValues();
        values.put("id", adminId);
        values.put("email", ADMIN_EMAIL);
        values.put("password", authService.hashPassword(ADMIN_PASSWORD));
        values.put("full_name", "System Administrator");
        values.put("date_of_birth", getRandomBirthDate(30, 50));
        values.put("user_type", UserType.SUPER_USER.name());
        values.put("created_at", System.currentTimeMillis());
        values.put("updated_at", System.currentTimeMillis());

        db.insert("users", null, values);
        return adminId;
    }

    private String[] createDonorAccounts(SQLiteDatabase db) throws Exception {
        String[] donorIds = new String[DONOR_ACCOUNTS.length];

        for (int i = 0; i < DONOR_ACCOUNTS.length; i++) {
            String donorId = UUID.randomUUID().toString();
            donorIds[i] = donorId;

            ContentValues values = new ContentValues();
            values.put("id", donorId);
            values.put("email", DONOR_ACCOUNTS[i][0]);
            values.put("password", authService.hashPassword(DONOR_ACCOUNTS[i][1]));
            values.put("full_name", DONOR_ACCOUNTS[i][2]);
            values.put("date_of_birth", getRandomBirthDate(18, 65));
            values.put("blood_type", DONOR_ACCOUNTS[i][3]);
            values.put("user_type", UserType.DONOR.name());
            values.put("gender", Math.random() < 0.5 ? "M" : "F");
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            db.insert("users", null, values);
        }

        return donorIds;
    }

    private String[] createManagerAccounts(SQLiteDatabase db) throws Exception {
        String[] managerIds = new String[MANAGER_ACCOUNTS.length];

        for (int i = 0; i < MANAGER_ACCOUNTS.length; i++) {
            String managerId = UUID.randomUUID().toString();
            managerIds[i] = managerId;

            ContentValues values = new ContentValues();
            values.put("id", managerId);
            values.put("email", MANAGER_ACCOUNTS[i][0]);
            values.put("password", authService.hashPassword(MANAGER_ACCOUNTS[i][1]));
            values.put("full_name", MANAGER_ACCOUNTS[i][2]);
            values.put("phone_number", MANAGER_ACCOUNTS[i][3]);
            values.put("date_of_birth", getRandomBirthDate(25, 55));
            values.put("user_type", UserType.SITE_MANAGER.name());
            values.put("gender", Math.random() < 0.5 ? "M" : "F");
            values.put("created_at", System.currentTimeMillis());
            values.put("updated_at", System.currentTimeMillis());

            db.insert("users", null, values);
        }

        return managerIds;
    }

    private void createEvents(SQLiteDatabase db, String[] managerIds) throws Exception {
        for (int i = 0; i < EVENT_DATA.length; i++) {
            // Create location
            String locationId = UUID.randomUUID().toString();
            double lat = BASE_LAT + (Math.random() * COORD_SPREAD * 2 - COORD_SPREAD);
            double lng = BASE_LNG + (Math.random() * COORD_SPREAD * 2 - COORD_SPREAD);

            ContentValues locationValues = new ContentValues();
            locationValues.put("id", locationId);
            locationValues.put("address", EVENT_DATA[i][2]);
            locationValues.put("latitude", lat);
            locationValues.put("longitude", lng);
            locationValues.put("description", "Main entrance, look for blood drive signs");
            locationValues.put("created_at", System.currentTimeMillis());
            locationValues.put("updated_at", System.currentTimeMillis());

            db.insert("locations", null, locationValues);

            // Create event
            String eventId = UUID.randomUUID().toString();
            Calendar startTime = Calendar.getInstance();
            startTime.add(Calendar.DAY_OF_MONTH, i * 2 + 1);
            Calendar endTime = (Calendar) startTime.clone();
            endTime.add(Calendar.HOUR, 8);

            // Create blood type targets with proper JSON structure
            JSONObject bloodTypeTargets = new JSONObject();
            String[] bloodTypes = EVENT_DATA[i][3].equals("ALL") ?
                    new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"} :
                    EVENT_DATA[i][3].split(",");

            double totalGoal = Double.parseDouble(EVENT_DATA[i][4]);
            double goalPerType = totalGoal / bloodTypes.length;

            for (String bloodType : bloodTypes) {
                bloodTypeTargets.put(bloodType, goalPerType);
            }

            // Create mock collected amounts (30-70% of target)
            JSONObject bloodCollected = new JSONObject();
            for (String bloodType : bloodTypes) {
                double target = goalPerType;
                double collected = target * (0.3 + Math.random() * 0.4); // 30-70% of target
                bloodCollected.put(bloodType, collected);
            }

            ContentValues eventValues = new ContentValues();
            eventValues.put("id", eventId);
            eventValues.put("title", EVENT_DATA[i][0]);
            eventValues.put("description", EVENT_DATA[i][1]);
            eventValues.put("start_time", startTime.getTimeInMillis());
            eventValues.put("end_time", endTime.getTimeInMillis());
            eventValues.put("blood_type_targets", bloodTypeTargets.toString());
            eventValues.put("blood_collected", bloodCollected.toString());
            eventValues.put("host_id", managerIds[i % managerIds.length]);
            eventValues.put("status", EventStatus.UPCOMING.name());
            eventValues.put("location_id", locationId);
            eventValues.put("donation_start_time", "09:00");
            eventValues.put("donation_end_time", "17:00");
            eventValues.put("created_at", System.currentTimeMillis());
            eventValues.put("updated_at", System.currentTimeMillis());

            db.insert("events", null, eventValues);

            // Create mock registrations
            createMockRegistrations(db, eventId);
        }
    }

    private void createMockRegistrations(SQLiteDatabase db, String eventId) throws Exception {
        // Create 5-15 donor registrations
        int donorCount = 5 + (int)(Math.random() * 10);
        for (int i = 0; i < donorCount; i++) {
            String registrationId = UUID.randomUUID().toString();

            ContentValues values = new ContentValues();
            values.put("registration_id", registrationId);
            values.put("user_id", UUID.randomUUID().toString()); // Random donor ID
            values.put("event_id", eventId);
            values.put("type", RegistrationType.DONOR.name());
            values.put("registration_time", System.currentTimeMillis());
            values.put("status", "ACTIVE");

            db.insert("registrations", null, values);
        }

        // Create 2-5 volunteer registrations
        int volunteerCount = 2 + (int)(Math.random() * 3);
        for (int i = 0; i < volunteerCount; i++) {
            String registrationId = UUID.randomUUID().toString();

            ContentValues values = new ContentValues();
            values.put("registration_id", registrationId);
            values.put("user_id", UUID.randomUUID().toString()); // Random volunteer ID
            values.put("event_id", eventId);
            values.put("type", RegistrationType.VOLUNTEER.name());
            values.put("registration_time", System.currentTimeMillis());
            values.put("status", "ACTIVE");

            db.insert("registrations", null, values);
        }
    }

    private long getRandomBirthDate(int minAge, int maxAge) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -minAge - (int)(Math.random() * (maxAge - minAge)));
        return cal.getTimeInMillis();
    }
}
