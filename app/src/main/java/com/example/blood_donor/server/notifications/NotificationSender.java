package com.example.blood_donor.server.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationSender {
    public static void send(Context context, NotificationTemplate template, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") Notification notification = new Notification.Builder(context, template.getChannelId())
                    .setContentTitle(template.getTitle())
                    .setContentText(template.getContent())
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(template.getPriority())
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify(notificationId, notification);
        }
    }
}
