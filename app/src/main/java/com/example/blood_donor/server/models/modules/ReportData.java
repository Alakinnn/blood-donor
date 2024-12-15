package com.example.blood_donor.server.models.modules;

import java.util.HashMap;
import java.util.Map;

public class ReportData {
    private final Map<String, Object> metrics = new HashMap<>();

    public void addMetric(String key, Object value) {
        metrics.put(key, value);
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }
}
