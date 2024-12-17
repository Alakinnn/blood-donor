package com.example.blood_donor.ui.manager;

import android.content.Context;

import com.example.blood_donor.server.database.DatabaseHelper;
import com.example.blood_donor.server.notifications.EventNotificationHandler;
import com.example.blood_donor.server.repositories.*;
import com.example.blood_donor.server.services.*;
import com.example.blood_donor.server.services.interfaces.*;

public class ServiceLocator {
    private static Context applicationContext;
    private static volatile DatabaseHelper databaseHelper;
    private static volatile IUserRepository userRepository;
    private static volatile ISessionRepository sessionRepository;
    private static volatile IAuthService authService;
    private static volatile IUserService userService;
    private static volatile IEventRepository eventRepository;
    private static volatile ILocationRepository locationRepository;
    private static volatile EventService eventService;
    private static volatile DonationRegistrationService donationRegistrationService;
    private static volatile ManagerService managerService;
    private static volatile EventNotificationHandler eventNotificationHandler;
    private static volatile EventCacheService eventCacheService;
    private static volatile IAnalyticsService analyticsService;
    private static volatile IRegistrationRepository registrationRepository;

    private static final Object LOCK = new Object();

    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static synchronized DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            synchronized (LOCK) {
                if (databaseHelper == null) {
                    if (applicationContext == null) {
                        throw new IllegalStateException("ServiceLocator must be initialized first");
                    }
                    databaseHelper = DatabaseHelper.getInstance(applicationContext);
                }
            }
        }
        return databaseHelper;
    }

    public static synchronized IUserRepository getUserRepository() {
        if (userRepository == null) {
            synchronized (LOCK) {
                if (userRepository == null) {
                    userRepository = new UserRepository(getDatabaseHelper());
                }
            }
        }
        return userRepository;
    }

    public static synchronized ISessionRepository getSessionRepository() {
        if (sessionRepository == null) {
            synchronized (LOCK) {
                if (sessionRepository == null) {
                    sessionRepository = new SessionRepository(getDatabaseHelper());
                }
            }
        }
        return sessionRepository;
    }

    public static synchronized IAuthService getAuthService() {
        if (authService == null) {
            synchronized (LOCK) {
                if (authService == null) {
                    authService = new AuthService();
                }
            }
        }
        return authService;
    }

    public static synchronized IUserService getUserService() {
        if (userService == null) {
            synchronized (LOCK) {
                if (userService == null) {
                    userService = new UserService(
                            getDatabaseHelper(),
                            getUserRepository(),
                            getSessionRepository(),
                            getAuthService()
                    );
                }
            }
        }
        return userService;
    }

    public static synchronized IEventRepository getEventRepository() {
        if (eventRepository == null) {
            synchronized (LOCK) {
                if (eventRepository == null) {
                    eventRepository = new EventRepository(getDatabaseHelper());
                }
            }
        }
        return eventRepository;
    }

    public static synchronized ILocationRepository getLocationRepository() {
        if (locationRepository == null) {
            synchronized (LOCK) {
                if (locationRepository == null) {
                    locationRepository = new LocationRepository(getDatabaseHelper());
                }
            }
        }
        return locationRepository;
    }

    public static synchronized EventService getEventService() {
        if (eventService == null) {
            synchronized (LOCK) {
                if (eventService == null) {
                    eventService = new EventService(
                            getEventRepository(),
                            getEventCacheService(),
                            getLocationRepository(),
                            getUserRepository(),
                            getRegistrationRepository()
                    );
                }
            }
        }
        return eventService;
    }

    public static synchronized EventCacheService getEventCacheService() {
        if (eventCacheService == null) {
            synchronized (LOCK) {
                if (eventCacheService == null) {
                    eventCacheService = new EventCacheService();
                }
            }
        }
        return eventCacheService;
    }

    public static synchronized DonationRegistrationService getDonationRegistrationService() {
        if (donationRegistrationService == null) {
            synchronized (LOCK) {
                if (donationRegistrationService == null) {
                    donationRegistrationService = new DonationRegistrationService(
                            getRegistrationRepository(),
                            getUserRepository(),
                            getEventRepository()
                    );
                }
            }
        }
        return donationRegistrationService;
    }

    public static synchronized ManagerService getManagerService() {
        if (managerService == null) {
            synchronized (LOCK) {
                if (managerService == null) {
                    managerService = new ManagerService(
                            getEventRepository(),
                            getUserRepository(),
                            getRegistrationRepository(), // Changed from sessionRepository
                            getEventService()
                    );
                }
            }
        }
        return managerService;
    }

    public static synchronized EventNotificationHandler getEventNotificationHandler() {
        if (eventNotificationHandler == null) {
            synchronized (LOCK) {
                if (eventNotificationHandler == null) {
                    eventNotificationHandler = new EventNotificationHandler(applicationContext);
                }
            }
        }
        return eventNotificationHandler;
    }

    public static synchronized IAnalyticsService getAnalyticsService() {
        if (analyticsService == null) {
            synchronized (LOCK) {
                if (analyticsService == null) {
                    analyticsService = new AnalyticsService(
                            getEventRepository(),
                            getRegistrationRepository(), // Changed from sessionRepository
                            getUserRepository()
                    );
                }
            }
        }
        return analyticsService;
    }

    public static void reset() {
        synchronized (LOCK) {
            if (userService != null) {
                userService.shutdown();
            }

            userRepository = null;
            sessionRepository = null;
            authService = null;
            userService = null;
            eventRepository = null;
            locationRepository = null;
            eventService = null;
            donationRegistrationService = null;
            managerService = null;
            eventNotificationHandler = null;
            eventCacheService = null;
            analyticsService = null;
        }
    }

    public static synchronized IRegistrationRepository getRegistrationRepository() {
        if (registrationRepository == null) {
            synchronized (LOCK) {
                if (registrationRepository == null) {
                    registrationRepository = new RegistrationRepository(getDatabaseHelper());
                }
            }
        }
        return registrationRepository;
    }
}