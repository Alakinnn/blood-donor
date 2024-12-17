package com.example.blood_donor;

import android.app.Application;

import com.android.volley.BuildConfig;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.ui.utils.DatabaseSeeder;

public class BloodDonorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AuthManager.getInstance().init(
                getSharedPreferences("AuthManager", MODE_PRIVATE)
        );
        ServiceLocator.init(this);
        // Seed database in debug mode
        if (BuildConfig.DEBUG) {
            // Run on background thread to avoid blocking main thread
            new Thread(() -> {
                DatabaseSeeder seeder = new DatabaseSeeder(ServiceLocator.getDatabaseHelper());
                seeder.seedDatabase();
            }).start();
        }
    }
}