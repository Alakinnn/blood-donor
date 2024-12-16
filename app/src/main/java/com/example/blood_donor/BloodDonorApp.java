package com.example.blood_donor;

import android.app.Application;

import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;

public class BloodDonorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AuthManager.getInstance().init(
                getSharedPreferences("AuthManager", MODE_PRIVATE)
        );
        ServiceLocator.init(this);
    }
}