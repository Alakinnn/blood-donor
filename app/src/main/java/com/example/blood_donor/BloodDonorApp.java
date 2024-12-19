package com.example.blood_donor;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.ui.utils.DatabaseSeeder;

public class BloodDonorApp extends Application {
    private static final String PREF_NAME = "BloodDonorPrefs";
    private static final String KEY_DB_SEEDED = "database_seeded";

    @Override
    public void onCreate() {
        super.onCreate();
        initializeApp();
    }

    private void initializeApp() {
        Log.d("BloodDonorApp", "Starting app initialization...");

        // Initialize Auth Manager
        AuthManager.getInstance().init(
                getSharedPreferences("AuthManager", MODE_PRIVATE)
        );

        // Initialize Service Locator
        ServiceLocator.init(this);

        // Check if database needs seeding
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_DB_SEEDED, false)) {
            Log.d("BloodDonorApp", "Database needs seeding, starting seeder...");
            seedDatabase(prefs);
        } else {
            Log.d("BloodDonorApp", "Database already seeded according to preferences");
        }
    }

    private void seedDatabase(SharedPreferences prefs) {
        new Thread(() -> {
            try {
                Log.d("BloodDonorApp", "Starting database seeding process");
                DatabaseSeeder seeder = new DatabaseSeeder(ServiceLocator.getDatabaseHelper());
                seeder.seedDatabase();

                // Verify database content after seeding
                ServiceLocator.getDatabaseHelper().verifyDatabaseContent();

                // Mark database as seeded only after successful verification
                prefs.edit().putBoolean(KEY_DB_SEEDED, true).apply();
                Log.d("BloodDonorApp", "Database seeding completed and verified");
            } catch (Exception e) {
                Log.e("BloodDonorApp", "Error during database seeding", e);
                // Clear the seeded flag if there was an error
                prefs.edit().putBoolean(KEY_DB_SEEDED, false).apply();
            }
        }).start();
    }
}