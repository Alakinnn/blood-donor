package com.example.blood_donor.ui.manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.BaseActivity;
import com.example.blood_donor.ui.LoginActivity;

public class AuthManager {
    private static volatile AuthManager instance;
    private SharedPreferences prefs;
    private static final String PREF_AUTH = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_USER_NAME = "user_name";

    private AuthManager() {
        // Prevent instantiation
    }

    public static AuthManager getInstance() {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager();
                }
            }
        }
        return instance;
    }

    public void init(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void saveAuthToken(String token, String userId, UserType userType, String userName) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_TYPE, userType.name())
                .putString(KEY_USER_NAME, userName)
                .apply();
    }

    public void clearAuth() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_TYPE)
                .remove(KEY_USER_NAME)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs != null && prefs.contains(KEY_TOKEN);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public UserType getUserType() {
        String userType = prefs.getString(KEY_USER_TYPE, null);
        return userType != null ? UserType.valueOf(userType) : null;
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void navigateToAppropriateScreen(Context context) {
        Intent intent;
        if (isLoggedIn()) {
            // If logged in, go to BaseActivity (which contains all our fragments)
            intent = new Intent(context, BaseActivity.class);
        } else {
            // If not logged in, go to LoginActivity
            intent = new Intent(context, LoginActivity.class);
        }
        // Clear back stack and start fresh
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void logout(Context context) {
        clearAuth();
        navigateToAppropriateScreen(context);
    }
}