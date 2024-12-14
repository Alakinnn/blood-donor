package com.example.blood_donor.services;

import android.annotation.SuppressLint;

import com.example.blood_donor.models.donation.RegistrationType;
import com.example.blood_donor.models.event.DonationEvent;
import com.example.blood_donor.models.event.EventStatistics;
import com.example.blood_donor.models.event.EventStatus;
import com.example.blood_donor.models.exceptions.AnalyticsErrorCode;
import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.modules.ReportData;
import com.example.blood_donor.models.modules.ReportFormat;
import com.example.blood_donor.models.modules.ReportTimeframe;
import com.example.blood_donor.models.user.User;
import com.example.blood_donor.models.user.UserType;
import com.example.blood_donor.repositories.IEventRepository;
import com.example.blood_donor.repositories.IRegistrationRepository;
import com.example.blood_donor.repositories.IUserRepository;
import com.example.blood_donor.services.interfaces.IAnalyticsService;
import com.example.blood_donor.utils.CSVReportGenerator;
import com.example.blood_donor.utils.ExcelReportGenerator;
import com.example.blood_donor.utils.PDFReportGenerator;
import com.example.blood_donor.utils.ReportGenerator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsService implements IAnalyticsService {
    private final IEventRepository eventRepository;
    private final IRegistrationRepository registrationRepository;
    private final IUserRepository userRepository;

    private final Map<ReportFormat, ReportGenerator> generators;

    public AnalyticsService(
            IEventRepository eventRepository,
            IRegistrationRepository registrationRepository,
            IUserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;

        this.generators = new HashMap<>();
        generators.put(ReportFormat.CSV, new CSVReportGenerator());
        generators.put(ReportFormat.PDF, new PDFReportGenerator());
        generators.put(ReportFormat.EXCEL, new ExcelReportGenerator());
    }

    @Override
    public byte[] generateEventReport(String eventId, ReportFormat format) throws AnalyticsException {
        try {
            DonationEvent event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new AnalyticsException("Event not found"));

            EventStatistics stats = new EventStatistics(eventId);
            stats.setTotalDonors(registrationRepository.getRegistrationCount(eventId, RegistrationType.DONOR));
            stats.setTotalVolunteers(registrationRepository.getRegistrationCount(eventId, RegistrationType.VOLUNTEER));

            ReportData reportData = new ReportData();
            reportData.addMetric("Event Title", event.getTitle());
            reportData.addMetric("Total Donors", stats.getTotalDonors());
            reportData.addMetric("Total Volunteers", stats.getTotalVolunteers());
            reportData.addMetric("Blood Goal (L)", event.getBloodGoal());
            reportData.addMetric("Blood Collected (L)", event.getCurrentBloodCollected());
            reportData.addMetric("Achievement Rate",
                    String.format("%.1f%%", (event.getCurrentBloodCollected() / event.getBloodGoal()) * 100));

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            throw new AnalyticsException("Failed to generate event report", e);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public byte[] generateSiteManagerReport(String managerId, ReportFormat format) throws AnalyticsException {
        try {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new AnalyticsException(AnalyticsErrorCode.INVALID_USER));

            if (manager.getUserType() != UserType.SITE_MANAGER) {
                throw new AnalyticsException(AnalyticsErrorCode.INVALID_USER, "User is not a site manager");
            }

            List<DonationEvent> managerEvents = eventRepository.findEventsByHostId(managerId);

            if (managerEvents.isEmpty()) {
                throw new AnalyticsException(AnalyticsErrorCode.NO_DATA, "No events found for this manager");
            }

            ReportData reportData = new ReportData();
            reportData.addMetric("Manager Name", manager.getFullName());
            reportData.addMetric("Total Events", managerEvents.size());

            double totalBloodCollected = 0;
            double totalBloodGoal = 0;
            int totalDonors = 0;
            int totalVolunteers = 0;
            Map<String, Double> bloodTypeCollected = new HashMap<>();

            for (DonationEvent event : managerEvents) {
                totalBloodCollected += event.getCurrentBloodCollected();
                totalBloodGoal += event.getBloodGoal();

                totalDonors += registrationRepository.getRegistrationCount(
                        event.getEventId(),
                        RegistrationType.DONOR
                );

                totalVolunteers += registrationRepository.getRegistrationCount(
                        event.getEventId(),
                        RegistrationType.VOLUNTEER
                );

                // Aggregate blood types
                for (Map.Entry<String, Double> entry : event.getBloodTypeCollected().entrySet()) {
                    bloodTypeCollected.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }

            reportData.addMetric("Total Blood Collected (L)", totalBloodCollected);
            reportData.addMetric("Total Blood Goal (L)", totalBloodGoal);
            reportData.addMetric("Achievement Rate",
                    String.format("%.1f%%", (totalBloodCollected / totalBloodGoal) * 100));
            reportData.addMetric("Total Donors", totalDonors);
            reportData.addMetric("Total Volunteers", totalVolunteers);
            reportData.addMetric("Blood Types Collected", bloodTypeCollected);

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            if (e instanceof AnalyticsException) {
                throw (AnalyticsException) e;
            }
            throw new AnalyticsException(AnalyticsErrorCode.PROCESSING_ERROR, e.getMessage());
        }
    }

    @Override
    public byte[] generateSystemReport(ReportTimeframe timeframe, ReportFormat format) throws AnalyticsException {
        try {
            long startTime = calculateStartTime(timeframe);
            long endTime = System.currentTimeMillis();

            List<DonationEvent> events = eventRepository.findEventsBetween(startTime, endTime);
            List<User> users = userRepository.findUsersByTimeRange(startTime, endTime);

            ReportData reportData = new ReportData();
            reportData.addMetric("Report Timeframe", timeframe.toString());
            reportData.addMetric("Total Events", events.size());

            // User statistics
            long totalDonors = users.stream()
                    .filter(u -> u.getUserType() == UserType.DONOR)
                    .count();
            long totalManagers = users.stream()
                    .filter(u -> u.getUserType() == UserType.SITE_MANAGER)
                    .count();

            reportData.addMetric("Total Registered Donors", totalDonors);
            reportData.addMetric("Total Site Managers", totalManagers);

            // Event statistics
            double totalBloodCollected = events.stream()
                    .mapToDouble(DonationEvent::getCurrentBloodCollected)
                    .sum();
            double totalBloodGoal = events.stream()
                    .mapToDouble(DonationEvent::getBloodGoal)
                    .sum();

            reportData.addMetric("Total Blood Collected (L)", totalBloodCollected);
            reportData.addMetric("Total Blood Goal (L)", totalBloodGoal);
            reportData.addMetric("Overall Achievement Rate",
                    String.format("%.1f%%", (totalBloodCollected / totalBloodGoal) * 100));

            // Status breakdown
            Map<EventStatus, Long> statusCounts = events.stream()
                    .collect(Collectors.groupingBy(
                            DonationEvent::getStatus,
                            Collectors.counting()
                    ));
            reportData.addMetric("Event Status Breakdown", statusCounts);

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            throw new AnalyticsException("Failed to generate system report", e);
        }
    }

    private long calculateStartTime(ReportTimeframe timeframe) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        switch (timeframe) {
            case DAILY:
                cal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case MONTHLY:
                cal.add(Calendar.MONTH, -1);
                break;
            case YEARLY:
                cal.add(Calendar.YEAR, -1);
                break;
        }
        return cal.getTimeInMillis();
    }
}