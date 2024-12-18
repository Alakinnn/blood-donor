package com.example.blood_donor.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.models.donation.RegistrationType;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.services.DonationRegistrationService;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {
    private MaterialTextView titleView;
    private MaterialTextView dateTimeView;
    private MaterialTextView addressView;
    private MaterialTextView descriptionView;
    private MaterialTextView hostInfoView;
    private MaterialTextView requiredBloodTypesView;
    private MaterialTextView donorCountView;
    private MaterialTextView volunteerCountView;
    private LinearProgressIndicator progressBar;
    private MaterialTextView progressText;
    private MaterialButton actionButton;

    private final EventService eventService;
    private final DonationRegistrationService registrationService;
    private String eventId;
    private EventDetailDTO eventDetails;
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public EventDetailsActivity() {
        this.eventService = ServiceLocator.getEventService();
        this.registrationService = ServiceLocator.getDonationRegistrationService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventId = getIntent().getStringExtra("eventId");
        Log.d("EventDetailsActivity", "Opening details for event ID: " + eventId);

        if (eventId == null) {
            Log.e("EventDetailsActivity", "No event ID provided");
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadEventDetails();
    }

    private void initializeViews() {
        // Explicitly cast to MaterialTextView
        titleView = (MaterialTextView) findViewById(R.id.eventTitle);
        dateTimeView = (MaterialTextView) findViewById(R.id.eventDateTime);
        addressView = (MaterialTextView) findViewById(R.id.eventAddress);
        descriptionView = (MaterialTextView) findViewById(R.id.eventDescription);
        hostInfoView = (MaterialTextView) findViewById(R.id.hostInfo);
        requiredBloodTypesView = (MaterialTextView) findViewById(R.id.requiredBloodTypes);
        donorCountView = (MaterialTextView) findViewById(R.id.donorCount);
        volunteerCountView = (MaterialTextView) findViewById(R.id.volunteerCount);
        progressBar = findViewById(R.id.bloodProgress);
        progressText = (MaterialTextView) findViewById(R.id.progressText);
        actionButton = findViewById(R.id.actionButton);

        // Set up back button in toolbar
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void updateUI() {
        if (eventDetails == null) {
            Log.e("EventDetails", "Attempted to update UI with null event details");
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title with null check
        titleView.setText(eventDetails.getTitle() != null ?
                eventDetails.getTitle() : "Untitled Event");

        // Format and set date/time with null checks
        String dateTimeText = String.format("%s - %s",
                formatDate(eventDetails.getStartTime()),
                formatDate(eventDetails.getEndTime()));
        dateTimeView.setText(dateTimeText);

        // Set address with null check
        addressView.setText(eventDetails.getAddress() != null ?
                eventDetails.getAddress() : "Location not specified");

        // Set description with null check
        descriptionView.setText(eventDetails.getDescription() != null ?
                eventDetails.getDescription() : "No description available");

        // Set host info with null checks
        String hostInfo = "Hosted by ";
        if (eventDetails.getHostName() != null) {
            hostInfo += eventDetails.getHostName();
            if (eventDetails.getHostPhoneNumber() != null) {
                hostInfo += "\nContact: " + eventDetails.getHostPhoneNumber();
            }
        } else {
            hostInfo += "Unknown Host";
        }
        hostInfoView.setText(hostInfo);

        // Handle blood types with null check
        List<String> bloodTypes = eventDetails.getRequiredBloodTypes();
        String bloodTypeText = "Required Blood Types: ";
        if (bloodTypes != null && !bloodTypes.isEmpty()) {
            bloodTypeText += String.join(", ", bloodTypes);
        } else {
            bloodTypeText += "None specified";
        }
        requiredBloodTypesView.setText(bloodTypeText);

        // Set counts with null checks and default values
        donorCountView.setText(String.format(Locale.getDefault(),
                "%d donors registered", eventDetails.getDonorCount()));
        volunteerCountView.setText(String.format(Locale.getDefault(),
                "%d volunteers registered", eventDetails.getVolunteerCount()));

        // Set progress with null checks and bounds checking
        double progress = 0;
        if (eventDetails.getBloodGoal() > 0) {
            progress = (eventDetails.getCurrentBloodCollected() /
                    eventDetails.getBloodGoal()) * 100;
        }
        progressBar.setProgress((int) progress);
        progressText.setText(String.format(Locale.getDefault(),
                "%.1f%% of %.1fL goal",
                progress, eventDetails.getBloodGoal()));

        // Handle donation hours display with null checks
        if (eventDetails.getDonationStartTime() != null &&
                eventDetails.getDonationEndTime() != null) {
            String donationHours = String.format("Donation Hours: %s - %s",
                    eventDetails.getDonationStartTime().format(
                            DateTimeFormatter.ofPattern("h:mm a")),
                    eventDetails.getDonationEndTime().format(
                            DateTimeFormatter.ofPattern("h:mm a")));

            MaterialTextView donationHoursView = findViewById(R.id.donationHours);
            donationHoursView.setText(donationHours);
            donationHoursView.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.donationHours).setVisibility(View.GONE);
        }

        setupActionButton();
    }

    private String formatDate(long timeInMillis) {
        if (timeInMillis <= 0) {
            return "Time not specified";
        }
        return dateFormat.format(new Date(timeInMillis));
    }

    private void loadEventDetails() {
        String userId = AuthManager.getInstance().getUserId();
        Log.d("EventDetails", "Loading event with ID: " + eventId);

        try {
            ApiResponse<EventDetailDTO> response = eventService.getEventDetails(eventId, userId);

            if (response.isSuccess() && response.getData() != null) {
                eventDetails = response.getData();
                updateUI();
            } else {
                String errorMessage = response.getMessage() != null ?
                        response.getMessage() : "Error loading event details";
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e("EventDetails", "Error loading event details", e);
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupActionButton() {
        UserType userType = AuthManager.getInstance().getUserType();
        boolean isManager = userType == UserType.SITE_MANAGER;

        // Show different button text based on user type
        actionButton.setText(isManager ? "Join as Volunteer" : "Register to Donate");
        actionButton.setOnClickListener(v -> registerForEvent(isManager));
    }

    private void registerForEvent(boolean asVolunteer) {
        String userId = AuthManager.getInstance().getUserId();
        RegistrationType type = asVolunteer ?
                RegistrationType.VOLUNTEER : RegistrationType.DONOR;

        ApiResponse<Boolean> response =
                registrationService.register(userId, eventId);

        if (response.isSuccess()) {
            Toast.makeText(this, "Successfully registered!",
                    Toast.LENGTH_SHORT).show();
            loadEventDetails(); // Reload to update counts
        } else {
            Toast.makeText(this, response.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
