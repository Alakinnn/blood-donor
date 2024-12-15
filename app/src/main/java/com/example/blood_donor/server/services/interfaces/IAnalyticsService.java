package com.example.blood_donor.server.services.interfaces;

import com.example.blood_donor.server.models.exceptions.AnalyticsException;
import com.example.blood_donor.server.models.modules.ReportFormat;
import com.example.blood_donor.server.models.modules.ReportTimeframe;

public interface IAnalyticsService {
    byte[] generateEventReport(String eventId, ReportFormat format) throws AnalyticsException;
    byte[] generateSiteManagerReport(String managerId, ReportFormat format) throws AnalyticsException;
    byte[] generateSystemReport(ReportTimeframe timeframe, ReportFormat format) throws AnalyticsException;
}
