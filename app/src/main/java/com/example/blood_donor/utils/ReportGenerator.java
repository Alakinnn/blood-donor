package com.example.blood_donor.utils;

import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.modules.ReportData;

public interface ReportGenerator {
    byte[] generate(ReportData data) throws AnalyticsException;
}
