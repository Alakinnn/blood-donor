package com.example.blood_donor.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.ui.fragments.CreateEventFragment;
import com.example.blood_donor.ui.fragments.HistoryFragment;
import com.example.blood_donor.ui.fragments.HomeFragment;
import com.example.blood_donor.ui.fragments.MapFragment;
import com.example.blood_donor.ui.fragments.ProfileFragment;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.server.models.user.UserType;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.transition.platform.MaterialSharedAxis;

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
        if (userType == UserType.SITE_MANAGER || userType == UserType.SUPER_USER) {
            bottomNavigation.inflateMenu(R.menu.bottom_nav_manager);
        } else {
            bottomNavigation.inflateMenu(R.menu.bottom_nav_donor);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            final int itemId = item.getItemId();

            // Create appropriate fragment based on selected menu item
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_map) {
                fragment = new MapFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else if (itemId == R.id.nav_create_event &&
                    (AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER ||
                            AuthManager.getInstance().getUserType() == UserType.SUPER_USER)) {
                fragment = new CreateEventFragment();
            } else if (itemId == R.id.nav_history) {
                fragment = new HistoryFragment();
            }

            if (fragment != null) {
                // Configure the transition direction based on the current and new fragments
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment != null) {
                    int currentIndex = getFragmentIndex(currentFragment);
                    int newIndex = getFragmentIndex(fragment);
                    boolean forward = newIndex > currentIndex;

                    // Create transitions based on navigation direction
                    MaterialSharedAxis exitTransition = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
                    MaterialSharedAxis enterTransition = new MaterialSharedAxis(MaterialSharedAxis.X, forward);

                    currentFragment.setExitTransition(exitTransition);
                    fragment.setEnterTransition(enterTransition);
                }

                // Configure transition duration and load the fragment
                configureTransitions(fragment);
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    // Helper method to determine fragment index for transition direction
    private int getFragmentIndex(Fragment fragment) {
        if (fragment instanceof HomeFragment) return 0;
        if (fragment instanceof MapFragment) return 1;
        if (fragment instanceof CreateEventFragment) return 2;
        if (fragment instanceof HistoryFragment) return 3;
        if (fragment instanceof ProfileFragment) return 4;
        return 0;
    }

    private void loadFragment(Fragment fragment) {
        // Set up transitions
        MaterialSharedAxis exitTransition = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        MaterialSharedAxis enterTransition = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        MaterialSharedAxis returnTransition = new MaterialSharedAxis(MaterialSharedAxis.X, false);
        MaterialSharedAxis reenterTransition = new MaterialSharedAxis(MaterialSharedAxis.X, false);

        // Set transitions for the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            currentFragment.setExitTransition(exitTransition);
            currentFragment.setReturnTransition(returnTransition);
        }

        // Set transitions for the new fragment
        fragment.setEnterTransition(enterTransition);
        fragment.setReenterTransition(reenterTransition);

        // Perform fragment transaction with transitions
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Add helper method to configure transitions duration
    private void configureTransitions(Fragment fragment) {
        long duration = 300L; // Duration in milliseconds

        if (fragment.getEnterTransition() instanceof MaterialSharedAxis) {
            ((MaterialSharedAxis) fragment.getEnterTransition()).setDuration(duration);
        }
        if (fragment.getExitTransition() instanceof MaterialSharedAxis) {
            ((MaterialSharedAxis) fragment.getExitTransition()).setDuration(duration);
        }
        if (fragment.getReenterTransition() instanceof MaterialSharedAxis) {
            ((MaterialSharedAxis) fragment.getReenterTransition()).setDuration(duration);
        }
        if (fragment.getReturnTransition() instanceof MaterialSharedAxis) {
            ((MaterialSharedAxis) fragment.getReturnTransition()).setDuration(duration);
        }
    }
}