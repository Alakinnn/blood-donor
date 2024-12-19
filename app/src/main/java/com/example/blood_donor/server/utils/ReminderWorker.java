package com.example.blood_donor.server.utils;

import kotlinx.coroutines.scheduling.CoroutineScheduler;

public class ReminderWorker extends CoroutineScheduler.Worker {
    @Override
    public Result doWork() {
        String eventId = getInputData().getString("eventId");
        String eventTitle = getInputData().getString("eventTitle");
        String timeframe = getInputData().getString("timeframe");

        // Create and save notification
        // Send push notification

        return Result.success();
    }
}
