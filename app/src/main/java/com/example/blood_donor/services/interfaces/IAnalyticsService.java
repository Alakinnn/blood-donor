package com.example.blood_donor.services.interfaces;

import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.modules.ReportFormat;
import com.example.blood_donor.models.modules.ReportTimeframe;

public interface IAnalyticsService {
    byte[] generateEventReport(String eventId, ReportFormat format) throws AnalyticsException;
    byte[] generateSiteManagerReport(String managerId, ReportFormat format) throws AnalyticsException;
    byte[] generateSystemReport(ReportTimeframe timeframe, ReportFormat format) throws AnalyticsException;
}
