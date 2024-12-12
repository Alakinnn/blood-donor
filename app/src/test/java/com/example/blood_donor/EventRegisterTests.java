package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.errors.ErrorCode;
import com.example.blood_donor.models.donation.RegistrationType;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.event.EventStatus;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.models.response.ApiResponse;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.repositories.RegistrationRepository;
import com.example.blood_donor.services.RegistrationService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class EventRegisterTests {
    @Mock
    private IUserRepository userRepository;

    @Mock
    private IEventRepository eventRepository;

    private RegistrationRepository registrationRepository;
    private RegistrationService registrationService;
    private DatabaseHelper dbHelper;
    private Context context;

    // Test data
    private User testDonor;
    private User testManager;
    private DonationEvent activeEvent;
    private DonationEvent expiredEvent;
    private Location testLocation;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.application.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
        registrationRepository = new RegistrationRepository(dbHelper);

        registrationService = new RegistrationService(
                registrationRepository,
                userRepository,
                eventRepository
        );

        // Initialize test data
        setupTestData();
    }

    private void setupTestData() {
        // Create test location using Builder pattern
        testLocation = new Location.Builder()
                .locationId("loc123")
                .address("123 Test St")
                .coordinates(-33.865143, 151.209900)
                .description("Test Location")
                .build();

        // Create test users
        testDonor = new User(
                "donor123",
                "donor@test.com",
                "password",
                "Test Donor",
                System.currentTimeMillis(),
                "0400000000",
                UserType.DONOR,
                "A+",
                "M"
        );

        testManager = new User(
                "manager123",
                "manager@test.com",
                "password",
                "Test Manager",
                System.currentTimeMillis(),
                "0400000001",
                UserType.SITE_MANAGER,
                null,
                "F"
        );

        // Create test events
        long now = System.currentTimeMillis();
        activeEvent = new DonationEvent(
                "event123",
                "Active Event",
                "Test Description",
                now + 86400000, // starts in 1 day
                now + (2 * 86400000), // ends in 2 days
                testLocation,
                Arrays.asList("A+", "O-"),
                100.0,
                "host123"
        );

        expiredEvent = new DonationEvent(
                "event456",
                "Expired Event",
                "Test Description",
                now - (2 * 86400000), // started 2 days ago
                now - 86400000, // ended 1 day ago
                testLocation,
                Arrays.asList("A+", "O-"),
                100.0,
                "host123"
        );
    }

    @Test
    public void testDatabaseInitialization() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        assertTrue("Database should be opened successfully", db.isOpen());
        assertTrue("Registrations table should exist",
                dbHelper.isTableExists("registrations"));
    }

    @Test
    public void testDonorRegistrationForActiveEvent() throws AppException {
        // Arrange
        when(userRepository.findById(testDonor.getUserId()))
                .thenReturn(Optional.of(testDonor));
        when(eventRepository.findById(activeEvent.getEventId()))
                .thenReturn(Optional.of(activeEvent));

        // Act
        ApiResponse<Boolean> response = registrationService
                .register(testDonor.getUserId(), activeEvent.getEventId());

        // Assert
        assertTrue("Registration should be successful", response.isSuccess());
        assertFalse("Should indicate new registration", response.getData());
        assertTrue("Donor should be registered",
                registrationRepository.isRegistered(
                        testDonor.getUserId(),
                        activeEvent.getEventId()
                ));
    }

    @Test
    public void testManagerRegistrationAsVolunteer() throws AppException {
        // Arrange
        when(userRepository.findById(testManager.getUserId()))
                .thenReturn(Optional.of(testManager));
        when(eventRepository.findById(activeEvent.getEventId()))
                .thenReturn(Optional.of(activeEvent));

        // Act
        ApiResponse<Boolean> response = registrationService
                .register(testManager.getUserId(), activeEvent.getEventId());

        // Assert
        assertTrue("Registration should be successful", response.isSuccess());
        assertFalse("Should indicate new registration", response.getData());
        assertEquals("Should be registered as volunteer", 1,
                registrationRepository.getRegistrationCount(
                        activeEvent.getEventId(),
                        RegistrationType.VOLUNTEER
                ));
    }

    @Test
    public void testRegistrationForExpiredEvent() throws AppException {
        // Arrange
        when(userRepository.findById(testDonor.getUserId()))
                .thenReturn(Optional.of(testDonor));
        when(eventRepository.findById(expiredEvent.getEventId()))
                .thenReturn(Optional.of(expiredEvent));

        // Act
        ApiResponse<Boolean> response = registrationService
                .register(testDonor.getUserId(), expiredEvent.getEventId());

        // Assert
        assertFalse("Registration should fail", response.isSuccess());
        assertEquals("Should return correct error code",
                ErrorCode.INVALID_INPUT, response.getErrorCode());
        assertTrue("Error message should mention expiration",
                response.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    public void testAlreadyRegisteredReturnsSuccess() throws AppException {
        // Arrange
        when(userRepository.findById(testDonor.getUserId()))
                .thenReturn(Optional.of(testDonor));
        when(eventRepository.findById(activeEvent.getEventId()))
                .thenReturn(Optional.of(activeEvent));

        // First registration
        ApiResponse<Boolean> firstResponse = registrationService
                .register(testDonor.getUserId(), activeEvent.getEventId());

        // Act - Try to register again
        ApiResponse<Boolean> secondResponse = registrationService
                .register(testDonor.getUserId(), activeEvent.getEventId());

        // Assert
        assertTrue("Both attempts should return success", firstResponse.isSuccess());
        assertTrue("Both attempts should return success", secondResponse.isSuccess());
        assertFalse("First attempt should return false (new registration)",
                firstResponse.getData());
        assertTrue("Second attempt should return true (already registered)",
                secondResponse.getData());
    }

    @Test
    public void testRegistrationCountsByType() throws AppException {
        // Arrange
        when(userRepository.findById(any())).thenReturn(
                Optional.of(testDonor),
                Optional.of(testManager)
        );
        when(eventRepository.findById(activeEvent.getEventId()))
                .thenReturn(Optional.of(activeEvent));

        // Act - Register both a donor and a volunteer
        registrationService.register(testDonor.getUserId(), activeEvent.getEventId());
        registrationService.register(testManager.getUserId(), activeEvent.getEventId());

        // Assert
        assertEquals("Should have 1 donor registration", 1,
                registrationRepository.getRegistrationCount(
                        activeEvent.getEventId(),
                        RegistrationType.DONOR
                ));
        assertEquals("Should have 1 volunteer registration", 1,
                registrationRepository.getRegistrationCount(
                        activeEvent.getEventId(),
                        RegistrationType.VOLUNTEER
                ));
    }

    @Test
    public void testUserNotFound() throws AppException {
        // Arrange
        when(userRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act
        ApiResponse<Boolean> response = registrationService
                .register("nonexistent", activeEvent.getEventId());

        // Assert
        assertFalse("Registration should fail", response.isSuccess());
        assertEquals("Should return correct error code",
                ErrorCode.INVALID_INPUT, response.getErrorCode());
        assertTrue("Error message should mention user not found",
                response.getMessage().toLowerCase().contains("user not found"));
    }

    @Test
    public void testEventNotFound() throws AppException {
        // Arrange
        when(userRepository.findById(testDonor.getUserId()))
                .thenReturn(Optional.of(testDonor));
        when(eventRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act
        ApiResponse<Boolean> response = registrationService
                .register(testDonor.getUserId(), "nonexistent");

        // Assert
        assertFalse("Registration should fail", response.isSuccess());
        assertEquals("Should return correct error code",
                ErrorCode.INVALID_INPUT, response.getErrorCode());
        assertTrue("Error message should mention event not found",
                response.getMessage().toLowerCase().contains("event not found"));
    }

    @Test
    public void testRegistrationForCompletedEvent() throws AppException {
        // Arrange
        DonationEvent completedEvent = activeEvent;  // Use your test event
        completedEvent.setStatus(EventStatus.COMPLETED);

        when(userRepository.findById(testDonor.getUserId()))
                .thenReturn(Optional.of(testDonor));
        when(eventRepository.findById(completedEvent.getEventId()))
                .thenReturn(Optional.of(completedEvent));

        // Act
        ApiResponse<Boolean> response = registrationService
                .register(testDonor.getUserId(), completedEvent.getEventId());

        // Assert
        assertFalse("Registration should fail", response.isSuccess());
        assertEquals("Should return correct error code",
                ErrorCode.INVALID_INPUT, response.getErrorCode());
        assertTrue("Error message should mention completed status",
                response.getMessage().toLowerCase().contains("completed"));
    }
}