package com.example.blood_donor.ui.manager;
import android.content.SharedPreferences;

public class AuthManager {
    private static volatile AuthManager instance;
    private SharedPreferences prefs;
    private static final String PREF_AUTH = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";

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

    public void saveAuthToken(String token, String userId) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public void clearAuth() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
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
}