package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;

import com.example.blood_donor.database.DatabaseHelper;
import com.example.blood_donor.dto.events.CreateEventDTO;
import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.models.location.LatLng;
import com.example.blood_donor.services.LocationService;
import com.example.blood_donor.validators.LocationValidator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {33})
public class EventCreateTests {
    @Mock
    private Geocoder geocoder;

    private LocationService locationService;
    private LocationValidator locationValidator;
    private DatabaseHelper dbHelper;
    private Context context;

    // Test coordinates
    private static final double VALID_LAT = -33.865143;
    private static final double VALID_LNG = 151.209900;
    private static final String VALID_ADDRESS = "123 Test St, Sydney NSW 2000";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.application.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
        locationService = new LocationService(geocoder);
        locationValidator = new LocationValidator(locationService);
    }

    @Test
    public void testDatabaseLocationTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        assertTrue("Database should be opened successfully", db.isOpen());
        assertTrue("Locations table should exist",
                dbHelper.isTableExists(DatabaseHelper.TABLE_LOCATIONS));
    }

    @Test
    public void testLocationCreationWithValidData() {
        Location location = new Location.Builder()
                .locationId("test-id")
                .address(VALID_ADDRESS)
                .coordinates(VALID_LAT, VALID_LNG)
                .description("Test Location")
                .build();

        assertEquals(VALID_LAT, location.getLatitude(), 0.0001);
        assertEquals(VALID_LNG, location.getLongitude(), 0.0001);
        assertEquals(VALID_ADDRESS, location.getAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLocationCreationWithInvalidLatitude() {
        new Location.Builder()
                .locationId("test-id")
                .address(VALID_ADDRESS)
                .coordinates(91.0, VALID_LNG)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLocationCreationWithInvalidLongitude() {
        new Location.Builder()
                .locationId("test-id")
                .address(VALID_ADDRESS)
                .coordinates(VALID_LAT, 181.0)
                .build();
    }

    @Test
    public void testGeocodingSuccess() throws AppException, IOException {
        // Setup mock address
        Address mockAddress = mock(Address.class);
        when(mockAddress.getLatitude()).thenReturn(VALID_LAT);
        when(mockAddress.getLongitude()).thenReturn(VALID_LNG);
        when(geocoder.getFromLocationName(eq(VALID_ADDRESS), anyInt()))
                .thenReturn(Arrays.asList(mockAddress));

        Optional<LatLng> result = locationService.geocodeAddress(VALID_ADDRESS);
        assertTrue("Should return coordinates", result.isPresent());
        assertEquals(VALID_LAT, result.get().getLatitude(), 0.0001);
        assertEquals(VALID_LNG, result.get().getLongitude(), 0.0001);
    }

    @Test
    public void testGeocodingFailure() throws IOException, AppException {
        when(geocoder.getFromLocationName(anyString(), anyInt()))
                .thenReturn(Arrays.asList());

        Optional<LatLng> result = locationService.geocodeAddress("Invalid Address");
        assertFalse("Should return empty for invalid address", result.isPresent());
    }

    @Test
    public void testLocationValidation() {
        assertTrue(locationService.isValidLocation(VALID_LAT, VALID_LNG));
        assertFalse(locationService.isValidLocation(91.0, VALID_LNG));
        assertFalse(locationService.isValidLocation(VALID_LAT, 181.0));
    }

    @Test
    public void testDistanceCalculation() {
        // Distance between Sydney and Melbourne
        double distance = locationService.calculateDistance(
                VALID_LAT, VALID_LNG,    // Sydney
                -37.813628, 144.963058   // Melbourne
        );

        // Should be approximately 714km
        assertEquals(714.0, distance, 1.0);
    }

    @Test
    public void testLocationValidatorWithValidData() throws AppException, IOException {
        CreateEventDTO dto = new CreateEventDTO(
                "Test Event",
                "Description",
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000,
                100.0,
                Arrays.asList("A+", "O-"),
                VALID_ADDRESS,
                VALID_LAT,
                VALID_LNG,
                "Location description"
        );

        // Mock geocoding response
        Address mockAddress = mock(Address.class);
        when(mockAddress.getLatitude()).thenReturn(VALID_LAT);
        when(mockAddress.getLongitude()).thenReturn(VALID_LNG);
        when(geocoder.getFromLocationName(eq(VALID_ADDRESS), anyInt()))
                .thenReturn(Arrays.asList(mockAddress));

        // Should not throw exception
        locationValidator.validate(dto);
    }

    @Test(expected = AppException.class)
    public void testLocationValidatorWithInvalidAddress() throws AppException {
        CreateEventDTO dto = new CreateEventDTO(
                "Test Event",
                "Description",
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000,
                100.0,
                Arrays.asList("A+", "O-"),
                "",  // Invalid empty address
                VALID_LAT,
                VALID_LNG,
                "Location description"
        );

        locationValidator.validate(dto);
    }

    @Test(expected = AppException.class)
    public void testLocationValidatorWithInvalidCoordinates() throws AppException {
        CreateEventDTO dto = new CreateEventDTO(
                "Test Event",
                "Description",
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000,
                100.0,
                Arrays.asList("A+", "O-"),
                VALID_ADDRESS,
                91.0,  // Invalid latitude
                VALID_LNG,
                "Location description"
        );

        locationValidator.validate(dto);
    }
}