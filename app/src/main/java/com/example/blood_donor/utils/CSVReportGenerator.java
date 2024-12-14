package com.example.blood_donor.utils;

import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.modules.ReportData;

import java.util.Map;

public class CSVReportGenerator implements ReportGenerator {
    @Override
    public byte[] generate(ReportData data) throws AnalyticsException {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");

        for (Map.Entry<String, Object> entry : data.getMetrics().entrySet()) {
            csv.append(String.format("%s,%s\n",
                    escapeCSV(entry.getKey()),
                    escapeCSV(entry.getValue().toString())));
        }

        return csv.toString().getBytes();
    }

    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
