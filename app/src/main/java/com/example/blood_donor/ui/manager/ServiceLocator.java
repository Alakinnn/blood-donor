package com.example.blood_donor.ui.manager;

import android.content.Context;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.repositories.ISessionRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.SessionRepository;
import com.example.blood_donor.server.repositories.UserRepository;
import com.example.blood_donor.server.services.AuthService;
import com.example.blood_donor.server.services.UserService;
import com.example.blood_donor.server.services.interfaces.IAuthService;
import com.example.blood_donor.server.services.interfaces.IUserService;

public class ServiceLocator {
    private static Context applicationContext;
    private static DatabaseHelper databaseHelper;
    private static IUserRepository userRepository;
    private static ISessionRepository sessionRepository;
    private static IAuthService authService;
    private static IUserService userService;

    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
        databaseHelper = new DatabaseHelper(applicationContext);
    }

    public static synchronized DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            throw new IllegalStateException("ServiceLocator must be initialized first");
        }
        return databaseHelper;
    }

    public static synchronized IUserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new UserRepository(getDatabaseHelper());
        }
        return userRepository;
    }

    public static synchronized ISessionRepository getSessionRepository() {
        if (sessionRepository == null) {
            sessionRepository = new SessionRepository(getDatabaseHelper());
        }
        return sessionRepository;
    }

    public static synchronized IAuthService getAuthService() {
        if (authService == null) {
            authService = new AuthService();
        }
        return authService;
    }

    public static synchronized IUserService getUserService() {
        if (userService == null) {
            userService = new UserService(
                    getUserRepository(),
                    getSessionRepository(),
                    getAuthService()
            );
        }
        return userService;
    }

    public static void reset() {
        userRepository = null;
        sessionRepository = null;
        authService = null;
        userService = null;
    }
}