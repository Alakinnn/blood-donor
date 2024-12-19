package com.example.blood_donor.server.utils;

import com.example.blood_donor.server.models.exceptions.AnalyticsException;
import com.example.blood_donor.server.models.modules.ReportData;

public interface ReportGenerator {
    byte[] generate(ReportData data) throws AnalyticsException;
}
