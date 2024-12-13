package com.example.blood_donor.notifications;

import android.app.NotificationManager;

public class NotificationTemplate {
    private String title;
    private String content;
    private String channelId;
    private int priority;
    private long timestamp;

    private NotificationTemplate(Builder builder) {
        this.title = builder.title;
        this.content = builder.content;
        this.channelId = builder.channelId;
        this.priority = builder.priority;
        this.timestamp = builder.timestamp;
    }

    public static class Builder {
        private String title;
        private String content;
        private String channelId;
        private int priority = NotificationManager.IMPORTANCE_DEFAULT;
        private long timestamp = System.currentTimeMillis();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public NotificationTemplate build() {
            return new NotificationTemplate(this);
        }
    }

    // Getters
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getChannelId() { return channelId; }
    public int getPriority() { return priority; }
    public long getTimestamp() { return timestamp; }
}

