package com.example.blood_donor;

import android.app.Application;
import android.content.SharedPreferences;

import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.ui.utils.DatabaseSeeder;

public class BloodDonorApp extends Application {
    private static final String PREF_NAME = "BloodDonorPrefs";
    private static final String KEY_DB_SEEDED = "database_seeded";

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.init(this);
        initializeApp();
    }

    private void initializeApp() {
        // Initialize Auth Manager
        AuthManager.getInstance().init(
                getSharedPreferences("AuthManager", MODE_PRIVATE)
        );

        // Initialize Service Locator
        ServiceLocator.init(this);

        // Check if database needs seeding
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_DB_SEEDED, false)) {
            seedDatabase(prefs);
        }
    }

    private void seedDatabase(SharedPreferences prefs) {
        new Thread(() -> {
            DatabaseSeeder seeder = new DatabaseSeeder(ServiceLocator.getDatabaseHelper());
            seeder.seedDatabase();

            // Mark database as seeded
            prefs.edit().putBoolean(KEY_DB_SEEDED, true).apply();
        }).start();
    }
}