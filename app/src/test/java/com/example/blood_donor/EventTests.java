package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.dto.events.EventDetailDTO;
import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.dto.locations.EventSummaryDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.donation.RegistrationType;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.event.EventStatus;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.EventRepository;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.ILocationRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.repositories.RegistrationRepository;
import com.example.blood_donor.services.EventCacheService;
import com.example.blood_donor.services.EventService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class EventTests {
    @Mock
    private IEventRepository eventRepository;
    @Mock
    private ILocationRepository locationRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private EventCacheService eventCache;
    @Mock
    private DatabaseHelper dbHelper;
    @Mock
    private SQLiteDatabase db;

    private EventService eventService;
    private Location testLocation;
    private DonationEvent testEvent;
    private User testHost;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(dbHelper.getWritableDatabase()).thenReturn(db);
        when(dbHelper.getReadableDatabase()).thenReturn(db);

        // Initialize test location
        testLocation = new Location.Builder()
                .locationId("loc123")
                .address("123 Test St")
                .coordinates(-33.865143, 151.209900)
                .description("Test Location")
                .build();

        // Initialize test host
        testHost = new User(
                "host123",
                "host@test.com",
                "password",
                "Test Host",
                System.currentTimeMillis(),
                "0400000000",
                UserType.SITE_MANAGER,
                null,
                "M"
        );

        // Initialize test event
        testEvent = new DonationEvent(
                "event123",
                "Test Event",
                "Description",
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000,
                testLocation,
                Arrays.asList("A+", "O-"),
                100.0,
                testHost.getUserId()
        );

        // Initialize EventService with all mocked dependencies
        eventService = new EventService(
                eventRepository,
                eventCache,
                locationRepository,
                userRepository,
                registrationRepository
        );
    }



    @Test
    public void testGetEventSummaries() throws AppException {
        // Arrange
        EventQueryDTO query = new EventQueryDTO(
                -33.865143, // latitude
                151.209900, // longitude
                15.0,       // zoom level
                null,       // no search term
                null,       // no blood type filter
                "distance", // sort by distance
                "asc",     // ascending order
                1,         // page
                10         // page size
        );

        List<DonationEvent> mockEvents = Arrays.asList(testEvent);
        when(eventRepository.findEvents(any(EventQueryDTO.class)))
                .thenReturn(mockEvents);

        // Act
        ApiResponse<List<EventSummaryDTO>> response =
                eventService.getEventSummaries(query);

        // Assert
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response data should not be null", response.getData());
        assertEquals("Should return one event", 1, response.getData().size());

        EventSummaryDTO summary = response.getData().get(0);
        assertEquals("Event ID should match", testEvent.getEventId(),
                summary.getEventId());
        assertEquals("Title should match", testEvent.getTitle(),
                summary.getTitle());
        assertEquals("Blood goal should match", testEvent.getBloodGoal(),
                summary.getBloodGoal(), 0.01);
    }

    @Test
    public void testGetEventDetails_FromCache() throws AppException {
        // Set up mocks
        when(eventCache.getCachedEventDetails(testEvent.getEventId()))
                .thenReturn(Optional.of(testEvent));
        when(userRepository.findById(testHost.getUserId()))
                .thenReturn(Optional.of(testHost));
        when(registrationRepository.getRegistrationCount(testEvent.getEventId(),
                RegistrationType.DONOR)).thenReturn(5);
        when(registrationRepository.getRegistrationCount(testEvent.getEventId(),
                RegistrationType.VOLUNTEER)).thenReturn(2);

        // Act
        ApiResponse<EventDetailDTO> response =
                eventService.getEventDetails(testEvent.getEventId(), null);

        // Assert
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response data should not be null", response.getData());
        assertEquals("Event ID should match", testEvent.getEventId(),
                response.getData().getEventId());
        assertEquals("Host name should match", testHost.getFullName(),
                response.getData().getHostName());
        assertEquals("Donor count should match", 5, response.getData().getDonorCount());
        assertEquals("Volunteer count should match", 2,
                response.getData().getVolunteerCount());
    }

    @Test
    public void testGetEventDetails_FromRepository() throws AppException {
        // Set up mocks
        when(eventCache.getCachedEventDetails(testEvent.getEventId()))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(testEvent.getEventId()))
                .thenReturn(Optional.of(testEvent));
        when(userRepository.findById(testHost.getUserId()))
                .thenReturn(Optional.of(testHost));
        when(registrationRepository.getRegistrationCount(testEvent.getEventId(),
                RegistrationType.DONOR)).thenReturn(5);
        when(registrationRepository.getRegistrationCount(testEvent.getEventId(),
                RegistrationType.VOLUNTEER)).thenReturn(2);

        // Act
        ApiResponse<EventDetailDTO> response =
                eventService.getEventDetails(testEvent.getEventId(), null);

        // Assert
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response data should not be null", response.getData());
        assertEquals("Event ID should match", testEvent.getEventId(),
                response.getData().getEventId());
        assertEquals("Host name should match", testHost.getFullName(),
                response.getData().getHostName());
    }

    @Test
    public void testDistanceCalculation() {
        // Create a subclass for testing since calculateDistance is protected
        class TestEventRepository extends EventRepository {
            public TestEventRepository(DatabaseHelper dbHelper) {
                super(dbHelper);
            }

            // Expose the protected method for testing
            public double publicCalculateDistance(double lat1, double lon1, double lat2, double lon2) {
                return calculateDistance(lat1, lon1, lat2, lon2);
            }
        }

        TestEventRepository repository = new TestEventRepository(dbHelper);

        double lat1 = -33.865143; // Sydney
        double lon1 = 151.209900;
        double lat2 = -37.813628; // Melbourne
        double lon2 = 144.963058;

        double distance = repository.publicCalculateDistance(lat1, lon1, lat2, lon2);
        assertEquals("Distance between Sydney and Melbourne should be ~714km",
                714.0, distance, 1.0);
    }

    @Test
    public void testEventFiltering() throws AppException {
        // Arrange
        EventQueryDTO query = new EventQueryDTO(
                null,                          // latitude
                null,                          // longitude
                null,                          // zoomLevel
                "Test Event",                  // searchTerm
                Arrays.asList("A+", "O-"),     // bloodTypes
                "date",                        // sortBy
                "asc",                         // sortOrder
                1,                            // page
                10                            // pageSize
        );

        when(eventRepository.findEvents(query))
                .thenReturn(Arrays.asList(testEvent));

        // Act
        ApiResponse<List<EventSummaryDTO>> response =
                eventService.getEventSummaries(query);

        // Assert
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response data should not be null", response.getData());
        assertFalse("Response data should not be empty",
                response.getData().isEmpty());
    }

    @Test
    public void testEventStatus() {
        // Arrange
        DonationEvent event = new DonationEvent(
                "event123",
                "Test Event",
                "Description",
                System.currentTimeMillis() - 86400000, // 1 day ago
                System.currentTimeMillis() + 86400000, // 1 day ahead
                testLocation,
                Arrays.asList("A+", "O-"),
                100.0,
                "host123"
        );

        // Assert
        assertEquals("New event should have UPCOMING status",
                EventStatus.UPCOMING, event.getStatus());

        // Act
        event.setStatus(EventStatus.IN_PROGRESS);

        // Assert
        assertEquals("Event status should be updated",
                EventStatus.IN_PROGRESS, event.getStatus());
    }

    @Test
    public void testLocationValidation() {
        // Assert valid coordinates
        assertDoesNotThrow(() -> new Location.Builder()
                .locationId("loc123")
                .address("123 Test St")
                .coordinates(-33.865143, 151.209900)
                .description("Valid Location")
                .build());

        // Test invalid latitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Location.Builder()
                    .locationId("loc123")
                    .address("123 Test St")
                    .coordinates(-91.0, 151.209900) // Invalid latitude
                    .description("Invalid Location")
                    .build();
        });
        assertTrue(exception.getMessage().toLowerCase().contains("latitude"));
    }

    private static void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    private static <T extends Exception> T assertThrows(
            Class<T> expectedType,
            Runnable runnable
    ) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (expectedType.isInstance(e)) {
                return expectedType.cast(e);
            }
        }
        fail("Expected " + expectedType.getSimpleName() + " but nothing was thrown");
        return null;
    }
}
