package com.example.blood_donor.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood_donor.R;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.donation.Registration;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class EventStatisticsActivity extends AppCompatActivity {
    private BarChart genderChart;
    private PieChart bloodTypeChart;
    private LineChart participationChart;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_statistics);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeCharts();
        loadEventStatistics();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void initializeCharts() {
        genderChart = findViewById(R.id.genderChart);
        bloodTypeChart = findViewById(R.id.bloodTypeChart);
        participationChart = findViewById(R.id.participationChart);
    }

    private void loadEventStatistics() {
        try {
            // Get all registrations for this event
            List<Registration> registrations = ServiceLocator.getRegistrationRepository()
                    .getEventRegistrations(eventId);
            Log.d("EventStatistics", "Found " + registrations.size() + " registrations");

            // Get user details for each registration
            Map<String, User> users = new HashMap<>();
            for (Registration reg : registrations) {
                Log.d("EventStatistics", "Processing registration: " + reg.getRegistrationId()
                        + ", User ID: " + reg.getUserId()
                        + ", Type: " + reg.getType());

                Optional<User> userOpt = ServiceLocator.getUserRepository().findById(reg.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    users.put(user.getUserId(), user);
                    Log.d("EventStatistics", "Found user: " + user.getFullName()
                            + ", Gender: " + user.getGender()
                            + ", Blood Type: " + user.getBloodType());
                } else {
                    Log.d("EventStatistics", "User not found for ID: " + reg.getUserId());
                }
            }
            Log.d("EventStatistics", "Found " + users.size() + " users with details");

            // Process data for charts
            Map<String, Integer> genderData = processGenderData(users.values());
            Log.d("EventStatistics", "Gender distribution: " + genderData);

            Map<String, Integer> bloodTypeData = processBloodTypeData(users.values());
            Log.d("EventStatistics", "Blood type distribution: " + bloodTypeData);

            setupGenderChart(genderData);
            setupBloodTypeChart(bloodTypeData);
            setupParticipationChart(processParticipationData(registrations));

        } catch (AppException e) {
            Log.e("EventStatistics", "Error loading statistics", e);
            Toast.makeText(this, "Error loading statistics", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, Integer> processGenderData(Collection<User> users) {
        Map<String, Integer> genderCounts = new HashMap<>();
        for (User user : users) {
            genderCounts.merge(user.getGender(), 1, Integer::sum);
        }
        return genderCounts;
    }

    private void setupGenderChart(Map<String, Integer> genderData) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : genderData.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Gender Distribution");
        dataSet.setColors(new int[]{Color.rgb(64, 89, 128), Color.rgb(149, 165, 124)});

        BarData barData = new BarData(dataSet);
        genderChart.setData(barData);

        XAxis xAxis = genderChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        genderChart.getDescription().setEnabled(false);
        genderChart.animateY(1000);
        genderChart.invalidate();
    }

    private Map<String, Integer> processBloodTypeData(Collection<User> users) {
        Map<String, Integer> bloodTypeCounts = new HashMap<>();
        for (User user : users) {
            if (user.getBloodType() != null) {
                bloodTypeCounts.merge(user.getBloodType(), 1, Integer::sum);
            }
        }
        return bloodTypeCounts;
    }

    private void setupBloodTypeChart(Map<String, Integer> bloodTypeData) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (bloodTypeData.isEmpty()) {
            // Add a single entry for "No Data"
            entries.add(new PieEntry(1, "No Data Available"));
        } else {
            for (Map.Entry<String, Integer> entry : bloodTypeData.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, bloodTypeData.isEmpty() ? "No Data" : "Blood Types");
        dataSet.setColors(bloodTypeData.isEmpty() ?
                new int[]{Color.LTGRAY} : ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        bloodTypeChart.setData(data);
        bloodTypeChart.getDescription().setEnabled(false);
        bloodTypeChart.setEntryLabelTextSize(12f);
        bloodTypeChart.setCenterText(bloodTypeData.isEmpty() ? "No Data Available" : "Blood Types");
        bloodTypeChart.setCenterTextSize(16f);
        bloodTypeChart.animateY(1000);
        bloodTypeChart.invalidate();
    }

    private Map<Long, int[]> processParticipationData(List<Registration> registrations) {
        TreeMap<Long, int[]> timelineData = new TreeMap<>();

        for (Registration reg : registrations) {
            long time = reg.getRegistrationTime();
            timelineData.putIfAbsent(time, new int[]{0, 0});

            if (reg.getType() == RegistrationType.DONOR) {
                timelineData.get(time)[0]++;
            } else {
                timelineData.get(time)[1]++;
            }
        }

        // Calculate cumulative totals
        int donorTotal = 0;
        int volunteerTotal = 0;
        Map<Long, int[]> cumulativeData = new TreeMap<>();

        for (Map.Entry<Long, int[]> entry : timelineData.entrySet()) {
            donorTotal += entry.getValue()[0];
            volunteerTotal += entry.getValue()[1];
            cumulativeData.put(entry.getKey(), new int[]{donorTotal, volunteerTotal});
        }

        return cumulativeData;
    }

    private void setupParticipationChart(Map<Long, int[]> participationData) {
        ArrayList<Entry> donorEntries = new ArrayList<>();
        ArrayList<Entry> volunteerEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Long, int[]> entry : participationData.entrySet()) {
            donorEntries.add(new Entry(index, entry.getValue()[0]));
            volunteerEntries.add(new Entry(index, entry.getValue()[1]));
            labels.add(formatDate(entry.getKey()));
            index++;
        }

        LineDataSet donorSet = new LineDataSet(donorEntries, "Donors");
        donorSet.setColor(Color.BLUE);
        donorSet.setCircleColor(Color.BLUE);

        LineDataSet volunteerSet = new LineDataSet(volunteerEntries, "Volunteers");
        volunteerSet.setColor(Color.RED);
        volunteerSet.setCircleColor(Color.RED);

        LineData lineData = new LineData(donorSet, volunteerSet);
        participationChart.setData(lineData);

        XAxis xAxis = participationChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);

        participationChart.getDescription().setEnabled(false);
        participationChart.animateX(1000);
        participationChart.invalidate();
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("MM/dd", Locale.getDefault())
                .format(new Date(timestamp));
    }
}