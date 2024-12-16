package com.example.blood_donor.server.services;

import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.event.BloodTypeRequirement;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.event.EventStatistics;
import com.example.blood_donor.server.models.event.EventStatus;
import com.example.blood_donor.server.models.exceptions.AnalyticsErrorCode;
import com.example.blood_donor.server.models.exceptions.AnalyticsException;
import com.example.blood_donor.server.models.modules.ReportData;
import com.example.blood_donor.server.models.modules.ReportFormat;
import com.example.blood_donor.server.models.modules.ReportTimeframe;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.repositories.IEventRepository;
import com.example.blood_donor.server.repositories.IRegistrationRepository;
import com.example.blood_donor.server.repositories.IUserRepository;
import com.example.blood_donor.server.services.interfaces.IAnalyticsService;
import com.example.blood_donor.server.utils.CSVReportGenerator;
import com.example.blood_donor.server.utils.ExcelReportGenerator;
import com.example.blood_donor.server.utils.PDFReportGenerator;
import com.example.blood_donor.server.utils.ReportGenerator;

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

    public AnalyticsService(IEventRepository eventRepository,
                            IRegistrationRepository registrationRepository,
                            IUserRepository userRepository) {
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
            reportData.addMetric("Total Blood Goal (L)", event.getTotalTargetAmount());
            reportData.addMetric("Total Blood Collected (L)", event.getTotalCollectedAmount());

            // Add blood type breakdown
            Map<String, BloodTypeRequirement> requirements = event.getBloodRequirements();
            requirements.forEach((type, req) -> {
                reportData.addMetric(type + " Goal (L)", req.getTargetAmount());
                reportData.addMetric(type + " Collected (L)", req.getCollectedAmount());
                reportData.addMetric(type + " Progress", String.format("%.1f%%", req.getProgress()));
            });

            double overallProgress = event.getTotalCollectedAmount() / event.getTotalTargetAmount() * 100;
            reportData.addMetric("Overall Progress", String.format("%.1f%%", overallProgress));

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            throw new AnalyticsException("Failed to generate event report", e);
        }
    }

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
                throw new AnalyticsException(AnalyticsErrorCode.NO_DATA, "No events found");
            }

            ReportData reportData = new ReportData();
            reportData.addMetric("Manager Name", manager.getFullName());
            reportData.addMetric("Total Events", managerEvents.size());

            Map<String, Double> totalsByBloodType = new HashMap<>();
            Map<String, Double> goalsByBloodType = new HashMap<>();
            int totalDonors = 0;
            int totalVolunteers = 0;

            for (DonationEvent event : managerEvents) {
                // Aggregate blood type data
                event.getBloodRequirements().forEach((type, req) -> {
                    totalsByBloodType.merge(type, req.getCollectedAmount(), Double::sum);
                    goalsByBloodType.merge(type, req.getTargetAmount(), Double::sum);
                });

                totalDonors += registrationRepository.getRegistrationCount(event.getEventId(), RegistrationType.DONOR);
                totalVolunteers += registrationRepository.getRegistrationCount(event.getEventId(), RegistrationType.VOLUNTEER);
            }

            reportData.addMetric("Total Donors", totalDonors);
            reportData.addMetric("Total Volunteers", totalVolunteers);

            // Add blood type statistics
            goalsByBloodType.forEach((type, goal) -> {
                double collected = totalsByBloodType.getOrDefault(type, 0.0);
                reportData.addMetric(type + " Total Goal (L)", goal);
                reportData.addMetric(type + " Total Collected (L)", collected);
                reportData.addMetric(type + " Achievement Rate", String.format("%.1f%%", (collected/goal) * 100));
            });

            // Overall statistics
            double totalCollected = totalsByBloodType.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalGoal = goalsByBloodType.values().stream().mapToDouble(Double::doubleValue).sum();
            reportData.addMetric("Overall Blood Goal (L)", totalGoal);
            reportData.addMetric("Overall Blood Collected (L)", totalCollected);
            reportData.addMetric("Overall Achievement Rate", String.format("%.1f%%", (totalCollected/totalGoal) * 100));

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            if (e instanceof AnalyticsException) throw (AnalyticsException) e;
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
            reportData.addMetric("New Donors", users.stream().filter(u -> u.getUserType() == UserType.DONOR).count());
            reportData.addMetric("New Site Managers", users.stream().filter(u -> u.getUserType() == UserType.SITE_MANAGER).count());

            // Blood collection statistics
            Map<String, Double> totalsByBloodType = new HashMap<>();
            Map<String, Double> goalsByBloodType = new HashMap<>();

            for (DonationEvent event : events) {
                event.getBloodRequirements().forEach((type, req) -> {
                    totalsByBloodType.merge(type, req.getCollectedAmount(), Double::sum);
                    goalsByBloodType.merge(type, req.getTargetAmount(), Double::sum);
                });
            }

            goalsByBloodType.forEach((type, goal) -> {
                double collected = totalsByBloodType.getOrDefault(type, 0.0);
                reportData.addMetric(type + " Total Goal (L)", goal);
                reportData.addMetric(type + " Total Collected (L)", collected);
                reportData.addMetric(type + " Achievement Rate", String.format("%.1f%%", (collected/goal) * 100));
            });

            // Overall statistics
            double totalCollected = totalsByBloodType.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalGoal = goalsByBloodType.values().stream().mapToDouble(Double::doubleValue).sum();
            reportData.addMetric("Overall Blood Goal (L)", totalGoal);
            reportData.addMetric("Overall Blood Collected (L)", totalCollected);
            reportData.addMetric("Overall Achievement Rate", String.format("%.1f%%", (totalCollected/totalGoal) * 100));

            // Event status breakdown
            Map<EventStatus, Long> statusCounts = events.stream()
                    .collect(Collectors.groupingBy(DonationEvent::getStatus, Collectors.counting()));
            reportData.addMetric("Event Status Breakdown", statusCounts);

            return generators.get(format).generate(reportData);
        } catch (Exception e) {
            throw new AnalyticsException("Failed to generate system report", e);
        }
    }

    private long calculateStartTime(ReportTimeframe timeframe) {
        Calendar cal = Calendar.getInstance();
        switch (timeframe) {
            case DAILY: cal.add(Calendar.DAY_OF_MONTH, -1); break;
            case WEEKLY: cal.add(Calendar.WEEK_OF_YEAR, -1); break;
            case MONTHLY: cal.add(Calendar.MONTH, -1); break;
            case YEARLY: cal.add(Calendar.YEAR, -1); break;
        }
        return cal.getTimeInMillis();
    }
}