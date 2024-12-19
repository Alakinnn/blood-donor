package com.example.blood_donor.server.services;

import com.example.blood_donor.server.models.notification.Notification;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.repositories.NotificationRepository;
import com.example.blood_donor.server.repositories.UserRepository;
import com.example.blood_donor.server.utils.ReminderWorker;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NotificationService {
//    private final NotificationRepository notificationRepository;
//    private final UserRepository userRepository;
//
//    public void sendMatchingBloodTypeNotification(String eventId, Set<String> requiredBloodTypes) {
//        // Find users with matching blood types
//        List<User> matchingUsers = userRepository.findUsersByBloodTypes(requiredBloodTypes);
//
//        for (User user : matchingUsers) {
//            Notification notification = new Notification(
//                    UUID.randomUUID().toString(),
//                    "New Blood Donation Event",
//                    "A new donation event needs your blood type!",
//                    NotificationType.MATCHING_BLOOD_TYPE,
//                    user.getUserId()
//            );
//            notificationRepository.saveNotification(notification);
//            sendPushNotification(notification);
//        }
//    }
//
//    public void scheduleEventReminders(String eventId, String eventTitle, long eventTime) {
//        // Schedule using WorkManager instead of ScheduledExecutorService
//        WorkManager workManager = WorkManager.getInstance(context);
//
//        // 24 hour reminder
//        Data reminderData = new Data.Builder()
//                .putString("eventId", eventId)
//                .putString("eventTitle", eventTitle)
//                .putString("timeframe", "24 hours")
//                .build();
//
//        OneTimeWorkRequest reminder24h = new OneTimeWorkRequest.Builder(ReminderWorker.class)
//                .setInitialDelay(eventTime - System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24))
//                .setInputData(reminderData)
//                .build();
//
//        workManager.enqueue(reminder24h);
//
//        // Similar for 1 hour reminder
//    }
}

