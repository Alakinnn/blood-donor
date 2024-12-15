package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.example.blood_donor.server.notifications.EventNotificationHandler;
import com.example.blood_donor.server.notifications.EventNotificationObserver;
import com.example.blood_donor.server.notifications.NotificationConstants;
import com.example.blood_donor.server.notifications.NotificationTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O})
public class NotificationTests {
    @Mock
    private NotificationManager notificationManager;

    @Mock
    private Context mockContext;

    @Mock
    private ApplicationInfo applicationInfo;

    private EventNotificationHandler notificationHandler;
    private EventNotificationObserver notificationObserver;
    private Context context;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup ApplicationInfo
        applicationInfo.targetSdkVersion = Build.VERSION_CODES.O;
        when(mockContext.getApplicationInfo()).thenReturn(applicationInfo);
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .thenReturn(notificationManager);

        notificationHandler = new EventNotificationHandler(mockContext);
        notificationObserver = new EventNotificationObserver(mockContext, "user123");
    }

    @Test
    public void testEventReminderScheduling() {
        long eventTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2);
        notificationHandler.scheduleEventReminders("event123", "Test Event", eventTime);

        // Verify notification channel creation
        verify(notificationManager).createNotificationChannel(any());
    }

    @Test
    public void testEventCreatedNotification() {
        notificationObserver.onEventCreated("event123", "Test Event");

        ArgumentCaptor<android.app.Notification> notificationCaptor =
                ArgumentCaptor.forClass(android.app.Notification.class);
        verify(notificationManager).notify(anyInt(), notificationCaptor.capture());

        android.app.Notification notification = notificationCaptor.getValue();
        assertEquals("New Blood Donation Event", notification.extras.getString(android.app.Notification.EXTRA_TITLE));
        assertTrue(notification.extras.getString(android.app.Notification.EXTRA_TEXT)
                .contains("Test Event"));
    }

    @Test
    public void testParticipantJoinedNotification() {
        notificationObserver.onParticipantJoined("event123", "John Doe");

        ArgumentCaptor<android.app.Notification> notificationCaptor =
                ArgumentCaptor.forClass(android.app.Notification.class);
        verify(notificationManager).notify(anyInt(), notificationCaptor.capture());

        android.app.Notification notification = notificationCaptor.getValue();
        assertEquals("New Participant", notification.extras.getString(android.app.Notification.EXTRA_TITLE));
        assertTrue(notification.extras.getString(android.app.Notification.EXTRA_TEXT)
                .contains("John Doe"));
    }

    @Test
    public void testEventUpdateNotification() {
        String updateDetails = "Event time changed to 2 PM";
        notificationObserver.onEventUpdated("event123", updateDetails);

        ArgumentCaptor<android.app.Notification> notificationCaptor =
                ArgumentCaptor.forClass(android.app.Notification.class);
        verify(notificationManager).notify(anyInt(), notificationCaptor.capture());

        android.app.Notification notification = notificationCaptor.getValue();
        assertEquals("Event Updated", notification.extras.getString(android.app.Notification.EXTRA_TITLE));
        assertEquals(updateDetails, notification.extras.getString(android.app.Notification.EXTRA_TEXT));
    }

    @Test
    public void testNotificationTemplate() {
        NotificationTemplate template = new NotificationTemplate.Builder()
                .title("Test Title")
                .content("Test Content")
                .channelId(NotificationConstants.EVENT_CHANNEL_ID)
                .priority(NotificationManager.IMPORTANCE_HIGH)
                .build();

        assertEquals("Test Title", template.getTitle());
        assertEquals("Test Content", template.getContent());
        assertEquals(NotificationConstants.EVENT_CHANNEL_ID, template.getChannelId());
        assertEquals(NotificationManager.IMPORTANCE_HIGH, template.getPriority());
        assertTrue(template.getTimestamp() > 0);
    }

    @Test
    public void testUniqueNotificationIds() {
        String eventId = "event123";
        String type1 = "create";
        String type2 = "join";

        // Generate IDs for different notification types
        int id1 = (type1 + eventId).hashCode();
        int id2 = (type2 + eventId).hashCode();

        // Verify different types generate different IDs
        assertNotEquals(id1, id2);
    }

    @Test
    public void testNotificationChannelCreation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArgumentCaptor<android.app.NotificationChannel> channelCaptor =
                    ArgumentCaptor.forClass(android.app.NotificationChannel.class);

            verify(notificationManager).createNotificationChannel(channelCaptor.capture());

            android.app.NotificationChannel channel = channelCaptor.getValue();
            assertEquals(NotificationConstants.EVENT_CHANNEL_ID, channel.getId());
            assertEquals(NotificationConstants.EVENT_CHANNEL_NAME, channel.getName());
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.getImportance());
        }
    }
}
