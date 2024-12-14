package com.example.blood_donor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.os.Build;

import com.example.blood_donor.errors.AppException;
import com.example.blood_donor.models.donation.RegistrationType;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.location.Location;
import com.example.blood_donor.models.modules.ReportFormat;
import com.example.blood_donor.models.modules.ReportTimeframe;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.IRegistrationRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.AnalyticsService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O})
public class AnalyticsReportTests {
    @Mock
    private IEventRepository eventRepository;
    @Mock private IRegistrationRepository registrationRepository;
    @Mock private IUserRepository userRepository;
    private AnalyticsService analyticsService;
    private User testManager;
    private List<DonationEvent> testEvents;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsService(
                eventRepository,
                registrationRepository,
                userRepository
        );
        setupTestData();
    }

    private void setupTestData() {
        testManager = new User(
                "manager123",
                "manager@test.com",
                "password",
                "Test Manager",
                System.currentTimeMillis(),
                "0400000000",
                UserType.SITE_MANAGER,
                null,
                "M"
        );

        testEvents = Arrays.asList(
                createTestEvent("event1", 100.0, 75.0),
                createTestEvent("event2", 150.0, 120.0),
                createTestEvent("event3", 80.0, 60.0)
        );
    }

    @Test
    public void testCSVReport() throws AnalyticsException, AppException {
        when(userRepository.findById(testManager.getUserId())).thenReturn(Optional.of(testManager));
        when(eventRepository.findEventsByHostId(testManager.getUserId())).thenReturn(testEvents);
        when(registrationRepository.getRegistrationCount(anyString(), eq(RegistrationType.DONOR))).thenReturn(10);
        when(registrationRepository.getRegistrationCount(anyString(), eq(RegistrationType.VOLUNTEER))).thenReturn(5);

        byte[] report = analyticsService.generateSiteManagerReport(testManager.getUserId(), ReportFormat.CSV);
        String csvContent = new String(report);

        assertTrue(csvContent.contains("Manager Name,Test Manager"));
        assertTrue(csvContent.contains("Total Events,3"));
        assertTrue(csvContent.contains("Total Blood Collected (L),255.0"));
        assertTrue(csvContent.contains("Total Donors,30"));
        assertTrue(csvContent.contains("Total Volunteers,15"));
    }

    @Test
    public void testPDFReport() throws AnalyticsException, AppException {
        when(userRepository.findById(testManager.getUserId())).thenReturn(Optional.of(testManager));
        when(eventRepository.findEventsByHostId(testManager.getUserId())).thenReturn(testEvents);
        when(registrationRepository.getRegistrationCount(anyString(), any())).thenReturn(5);

        byte[] report = analyticsService.generateSiteManagerReport(testManager.getUserId(), ReportFormat.PDF);
        assertNotNull(report);
        assertTrue(report.length > 0);

        // Verify PDF header
        assertTrue(new String(report).contains("%PDF-"));
    }

    @Test
    public void testExcelReport() throws AnalyticsException, AppException {
        when(userRepository.findById(testManager.getUserId())).thenReturn(Optional.of(testManager));
        when(eventRepository.findEventsByHostId(testManager.getUserId())).thenReturn(testEvents);
        when(registrationRepository.getRegistrationCount(anyString(), any())).thenReturn(5);

        byte[] report = analyticsService.generateSiteManagerReport(testManager.getUserId(), ReportFormat.EXCEL);
        assertNotNull(report);
        assertTrue(report.length > 0);

        // Verify Excel header
        assertTrue(new String(report).contains("PK"));
    }

    @Test
    public void testSystemWideReport() throws AnalyticsException, AppException {
        when(eventRepository.findEventsBetween(anyLong(), anyLong())).thenReturn(testEvents);
        when(userRepository.findUsersByTimeRange(anyLong(), anyLong()))
                .thenReturn(Arrays.asList(testManager, createTestDonor()));

        byte[] report = analyticsService.generateSystemReport(ReportTimeframe.MONTHLY, ReportFormat.CSV);
        String csvContent = new String(report);

        assertTrue(csvContent.contains("Report Timeframe,MONTHLY"));
        assertTrue(csvContent.contains("Total Events,3"));
        assertTrue(csvContent.contains("Total Registered Donors,1"));
        assertTrue(csvContent.contains("Total Site Managers,1"));
    }

    @Test(expected = AnalyticsException.class)
    public void testInvalidManagerReport() throws AnalyticsException, AppException {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        analyticsService.generateSiteManagerReport("invalid-id", ReportFormat.CSV);
    }

    @Test(expected = AnalyticsException.class)
    public void testReportWithNoEvents() throws AnalyticsException, AppException {
        when(userRepository.findById(testManager.getUserId())).thenReturn(Optional.of(testManager));
        when(eventRepository.findEventsByHostId(anyString())).thenReturn(Collections.emptyList());

        analyticsService.generateSiteManagerReport(testManager.getUserId(), ReportFormat.CSV);
    }

    @Test
    public void testReportWithLargeDataSet() throws AnalyticsException, AppException {
        List<DonationEvent> largeEventList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeEventList.add(createTestEvent("event" + i, 100.0, 75.0));
        }

        when(userRepository.findById(testManager.getUserId())).thenReturn(Optional.of(testManager));
        when(eventRepository.findEventsByHostId(testManager.getUserId())).thenReturn(largeEventList);
        when(registrationRepository.getRegistrationCount(anyString(), any())).thenReturn(5);

        byte[] report = analyticsService.generateSiteManagerReport(testManager.getUserId(), ReportFormat.CSV);
        assertNotNull(report);
        assertTrue(report.length > 0);
    }

    private DonationEvent createTestEvent(String id, double goal, double collected) {
        DonationEvent event = new DonationEvent(
                id,
                "Test Event " + id,
                "Description",
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000,
                createTestLocation(),
                Arrays.asList("A+", "O-"),
                goal,
                testManager.getUserId()
        );
        event.setCurrentBloodCollected(collected);
        Map<String, Double> bloodTypes = new HashMap<>();
        bloodTypes.put("A+", collected * 0.6);
        bloodTypes.put("O-", collected * 0.4);
        event.setBloodTypeCollected(bloodTypes);
        return event;
    }

    private Location createTestLocation() {
        return new Location.Builder()
                .locationId("loc123")
                .address("123 Test St")
                .coordinates(-33.865143, 151.209900)
                .description("Test Location")
                .build();
    }

    private User createTestDonor() {
        return new User(
                "donor123",
                "donor@test.com",
                "password",
                "Test Donor",
                System.currentTimeMillis(),
                null,
                UserType.DONOR,
                "A+",
                "F"
        );
    }
}