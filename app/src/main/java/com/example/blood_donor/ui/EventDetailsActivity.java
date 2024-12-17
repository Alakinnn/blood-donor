package com.example.blood_donor.ui;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
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
import java.util.Date;
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
        try {
            setContentView(R.layout.activity_event_details);

            String eventId = getIntent().getStringExtra("eventId");
            if (eventId == null) {
                Log.e(TAG, "No eventId provided in intent");
                Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Loading event details for ID: " + eventId);

            initializeViews();
            loadEventDetails();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            finish();
        }
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

    private void loadEventDetails() {
        try {
            String userId = AuthManager.getInstance().getUserId();
            Log.d(TAG, "Fetching details with userId: " + userId);

            ApiResponse<EventDetailDTO> response = eventService.getEventDetails(eventId, userId);

            if (response.isSuccess() && response.getData() != null) {
                eventDetails = response.getData();
                updateUI();
            } else {
                Log.e(TAG, "Failed to load event details: " + response.getMessage());
                Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading event details", e);
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
        titleView.setText(eventDetails.getTitle());
        dateTimeView.setText(String.format("%s - %s",
                dateFormat.format(new Date(eventDetails.getStartTime())),
                dateFormat.format(new Date(eventDetails.getEndTime()))
        ));
        addressView.setText(eventDetails.getAddress());
        descriptionView.setText(eventDetails.getDescription());

        hostInfoView.setText(String.format("Hosted by %s\nContact: %s",
                eventDetails.getHostName(),
                eventDetails.getHostPhoneNumber()
        ));

        requiredBloodTypesView.setText(String.join(", ",
                eventDetails.getRequiredBloodTypes()));

        donorCountView.setText(String.format(Locale.getDefault(),
                "%d donors registered", eventDetails.getDonorCount()));
        volunteerCountView.setText(String.format(Locale.getDefault(),
                "%d volunteers registered", eventDetails.getVolunteerCount()));

        // Set up progress
        double progress = (eventDetails.getCurrentBloodCollected() /
                eventDetails.getBloodGoal()) * 100;
        progressBar.setProgress((int) progress);
        progressText.setText(String.format(Locale.getDefault(),
                "%.1f%% of %.1fL goal",
                progress, eventDetails.getBloodGoal()
        ));

        setupActionButton();
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
