package com.example.blood_donor.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.ui.fragments.HomeFragment;
import com.example.blood_donor.ui.fragments.MapFragment;
import com.example.blood_donor.ui.fragments.ProfileFragment;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.server.models.user.UserType;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        setupBottomNavigation();

        // Load initial fragment if this is the first creation
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Set menu based on user type
        UserType userType = AuthManager.getInstance().getUserType();
        bottomNavigation.getMenu().clear();
        bottomNavigation.inflateMenu(userType == UserType.SITE_MANAGER ?
                R.menu.bottom_nav_manager : R.menu.bottom_nav_donor);

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            final int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_map) {
                fragment = new MapFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else if (itemId == R.id.nav_create_event &&
                    AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER) {
                // Launch CreateEventActivity
                startActivity(new Intent(this, CreateEventActivity.class));
                return true;
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}