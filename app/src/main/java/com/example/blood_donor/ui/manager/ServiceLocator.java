package com.example.blood_donor.ui.manager;

import android.content.Context;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.repositories.EventRepository;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.ILocationRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.ISessionRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.repositories.LocationRepository;
import com.example.blood_donor.server.repositories.RegistrationRepository;
import com.example.blood_donor.server.repositories.SessionRepository;
import com.example.blood_donor.server.repositories.UserRepository;
import com.example.blood_donor.server.services.AnalyticsService;
import com.example.blood_donor.server.services.AuthService;
import com.example.blood_donor.server.services.DonationRegistrationService;
import com.example.blood_donor.server.services.EventCacheService;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.server.services.ManagerService;
import com.example.blood_donor.server.services.SuperUserEventService;
import com.example.blood_donor.server.services.UserService;
import com.example.blood_donor.server.services.interfaces.IAnalyticsService;
import com.example.blood_donor.server.services.interfaces.IAuthService;
import com.example.blood_donor.server.services.interfaces.IUserService;

public class ServiceLocator {
    private static Context applicationContext;
    private static DatabaseHelper databaseHelper;
    private static IUserRepository userRepository;
    private static ISessionRepository sessionRepository;
    private static IAuthService authService;
    private static IUserService userService;
    private static ManagerService managerEventService;
    private static RegistrationRepository registrationRepository;
    private static EventService eventService;
    private static IEventRepository eventRepository;
    private static ILocationRepository locationRepository;
    private static DonationRegistrationService donationRegistrationService;
    private static IAnalyticsService analyticsService;
    private static SuperUserEventService superUserEventService;
    private static NotificationManager notificationManager;


    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
        databaseHelper = new DatabaseHelper(applicationContext);
    }

    public static synchronized NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = new NotificationManager(
                    applicationContext,
                    getDatabaseHelper()
            );
        }
        return notificationManager;
    }

    public static synchronized DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            throw new IllegalStateException("ServiceLocator must be initialized first");
        }
        return databaseHelper;
    }

    public static synchronized SuperUserEventService getSuperUserEventService() {
        if (superUserEventService == null) {
            superUserEventService = new SuperUserEventService(
                    getEventRepository(),
                    getRegistrationRepository(),
                    databaseHelper,
                    getUserRepository(),
                    getEventService()
            );
        }
        return superUserEventService;
    }

    public static synchronized DonationRegistrationService getDonationRegistrationService() {
        if (donationRegistrationService == null) {
            donationRegistrationService = new DonationRegistrationService(
                    getRegistrationRepository(),
                    getUserRepository(),
                    getEventRepository()
            );
        }
        return donationRegistrationService;
    }

    public static synchronized ManagerService getManagerService() {
        if (managerEventService == null) {
            managerEventService = new ManagerService(
                    getEventRepository(),
                    getUserRepository(),
                    getRegistrationRepository(),
                    getEventService()
            );
        }
        return managerEventService;
    }

    public static synchronized IRegistrationRepository getRegistrationRepository() {
        if (registrationRepository == null) {
            registrationRepository = new RegistrationRepository(getDatabaseHelper());
        }
        return registrationRepository;
    }

    public static synchronized EventService getEventService() {
        if (eventService == null) {
            eventService = new EventService(
                    getEventRepository(),
                    new EventCacheService(),
                    getLocationRepository(),
                    getUserRepository(),
                    getRegistrationRepository()
            );
        }
        return eventService;
    }

    public static synchronized IEventRepository getEventRepository() {
        if (eventRepository == null) {
            eventRepository = new EventRepository(getDatabaseHelper());
        }
        return eventRepository;
    }

    public static synchronized IAnalyticsService getAnalyticsService() {
        if (analyticsService == null) {
            analyticsService = new AnalyticsService(
                    getEventRepository(),
                    getRegistrationRepository(),
                    getUserRepository()
            );
        }
        return analyticsService;
    }

    public static synchronized ILocationRepository getLocationRepository() {
        if (locationRepository == null) {
            locationRepository = new LocationRepository(getDatabaseHelper());
        }
        return locationRepository;
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