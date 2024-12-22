package com.example.blood_donor.ui;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.BloodTypeProgress;
import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.dto.events.UpdateEventDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.donation.Registration;
import com.example.blood_donor.server.models.event.BloodTypeRequirement;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.manager.NotificationManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EditEventActivity extends AppCompatActivity implements OnMapReadyCallback {
    private EditText titleInput;
    private EditText descriptionInput;
    private MaterialButton startDateBtn;
    private MaterialButton endDateBtn;
    private MaterialButton startTimeBtn;
    private MaterialButton endTimeBtn;
    private EditText locationInput;
    private String eventId;
    private EventDetailDTO eventDetails;
    private final EventService eventService;
    private final Calendar startDate = Calendar.getInstance();
    private final Calendar endDate = Calendar.getInstance();
    private LocalTime startTime;
    private LocalTime endTime;
    private LinearLayout bloodTypeContainer;
    private final Map<String, EditText> bloodTypeInputs = new HashMap<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private GoogleMap map;
    private Marker currentLocationMarker;
    private double latitude;
    private double longitude;
    private String address;
    private TextView locationText;

    public EditEventActivity() {
        this.eventService = ServiceLocator.getEventService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize back button
        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());

        initializeViews();
        setupDateTimeListeners();
        setupMap();
        loadEventDetails();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Set initial location from event details
        if (eventDetails != null) {
            latitude = eventDetails.getLatitude();
            longitude = eventDetails.getLongitude();
            address = eventDetails.getAddress();

            LatLng location = new LatLng(latitude, longitude);
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            currentLocationMarker = map.addMarker(new MarkerOptions()
                    .position(location)
                    .title(eventDetails.getTitle()));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
            locationText.setText(address);
        }

        // Setup map click listener
        map.setOnMapClickListener(latLng -> {
            updateLocation(latLng);
        });
    }

    private void updateLocation(LatLng latLng) {
        latitude = latLng.latitude;
        longitude = latLng.longitude;

        // Update marker
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        currentLocationMarker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Event Location"));

        // Geocode the location
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address addr = addresses.get(0);
                address = addr.getAddressLine(0);
                locationText.setText(address);
            }
        } catch (IOException e) {
            Log.e("EditEventActivity", "Error geocoding location", e);
            Toast.makeText(this, "Error getting address", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to leave?")
                    .setPositiveButton("Leave", (dialog, which) -> finish())
                    .setNegativeButton("Stay", null)
                    .show();
        } else {
            finish();
        }
    }

    private void setupMap() {
        locationText = findViewById(R.id.locationText);
        // Get the SupportMapFragment and request notification when the map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }


    private boolean hasUnsavedChanges() {
        // Check title and description changes
        if (!titleInput.getText().toString().equals(eventDetails.getTitle()) ||
                !descriptionInput.getText().toString().equals(eventDetails.getDescription())) {
            return true;
        }

        // Check date/time changes
        if (startDate.getTimeInMillis() != eventDetails.getStartTime() ||
                endDate.getTimeInMillis() != eventDetails.getEndTime() ||
                !startTime.equals(eventDetails.getDonationStartTime()) ||
                !endTime.equals(eventDetails.getDonationEndTime())) {
            return true;
        }

        // Check location changes
        if (!address.equals(eventDetails.getAddress()) ||
                latitude != eventDetails.getLatitude() ||
                longitude != eventDetails.getLongitude()) {
            return true;
        }

        // Check blood type target changes
        Map<String, Double> currentTargets = new HashMap<>();
        for (BloodTypeProgress progress : eventDetails.getBloodProgress()) {
            currentTargets.put(progress.getBloodType(), progress.getTargetAmount());
        }

        for (Map.Entry<String, EditText> entry : bloodTypeInputs.entrySet()) {
            String bloodType = entry.getKey();
            String newValue = entry.getValue().getText().toString();
            if (!newValue.isEmpty()) {
                double newTarget = Double.parseDouble(newValue);
                if (!currentTargets.containsKey(bloodType) ||
                        Math.abs(currentTargets.get(bloodType) - newTarget) > 0.01) {
                    return true;
                }
            }
        }

        return false;
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        startDateBtn = findViewById(R.id.startDateBtn);
        endDateBtn = findViewById(R.id.endDateBtn);
        startTimeBtn = findViewById(R.id.startTimeBtn);
        endTimeBtn = findViewById(R.id.endTimeBtn);
        locationInput = findViewById(R.id.locationInput);
        locationInput.setFocusable(false);
        locationInput.setClickable(false);
        bloodTypeContainer = findViewById(R.id.bloodTypeContainer);

        findViewById(R.id.saveButton).setOnClickListener(v -> saveChanges());
        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());
    }

    private void setupDateTimeListeners() {
        startDateBtn.setOnClickListener(v -> showDatePicker(true));
        endDateBtn.setOnClickListener(v -> showDatePicker(false));
        startTimeBtn.setOnClickListener(v -> showTimePicker(true));
        endTimeBtn.setOnClickListener(v -> showTimePicker(false));
    }

    private void loadEventDetails() {
        ApiResponse<EventDetailDTO> response = eventService.getEventDetails(eventId, null);
        if (response.isSuccess() && response.getData() != null) {
            eventDetails = response.getData();
            populateFields();
        } else {
            Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateFields() {
        titleInput.setText(eventDetails.getTitle());
        descriptionInput.setText(eventDetails.getDescription());
        locationInput.setText(eventDetails.getAddress());

        // Set dates
        startDate.setTimeInMillis(eventDetails.getStartTime());
        endDate.setTimeInMillis(eventDetails.getEndTime());
        startDateBtn.setText(dateFormat.format(startDate.getTime()));
        endDateBtn.setText(dateFormat.format(endDate.getTime()));

        // Set times
        startTime = eventDetails.getDonationStartTime();
        endTime = eventDetails.getDonationEndTime();
        startTimeBtn.setText(startTime.toString());
        endTimeBtn.setText(endTime.toString());

        // Setup blood type inputs
        setupBloodTypeInputs();
    }

    private void setupBloodTypeInputs() {
        bloodTypeContainer.removeAllViews();
        bloodTypeInputs.clear();

        List<BloodTypeProgress> bloodProgress = eventDetails.getBloodProgress();
        Log.d("EditEvent", "Blood Progress: " + bloodProgress); // Debug log

        Map<String, Double> currentTargets = new HashMap<>();

        // Get current target amounts from blood progress
        if (bloodProgress != null) {
            for (BloodTypeProgress progress : bloodProgress) {
                String bloodType = progress.getBloodType();
                double target = progress.getTargetAmount();
                Log.d("EditEvent", "Blood Type: " + bloodType + ", Target: " + target); // Debug log
                if (bloodType != null) {  // Add null check
                    currentTargets.put(bloodType, target);
                }
            }
        }

        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String bloodType : bloodTypes) {
            View bloodTypeView = inflater.inflate(R.layout.blood_type_input_item, bloodTypeContainer, false);
            TextInputLayout inputLayout = bloodTypeView.findViewById(R.id.amountInputLayout);
            TextInputEditText amountInput = bloodTypeView.findViewById(R.id.amountInput);

            inputLayout.setHint(bloodType + " Target (L)");
            amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            // Pre-fill with current target (not collected amount)
            if (currentTargets.containsKey(bloodType)) {
                double target = currentTargets.get(bloodType);
                Log.d("EditEvent", "Setting " + bloodType + " input to: " + target); // Debug log
                amountInput.setText(String.format(Locale.getDefault(), "%.1f", target));
            } else {
                Log.d("EditEvent", "No target found for " + bloodType); // Debug log
            }

            bloodTypeInputs.put(bloodType, amountInput);
            bloodTypeContainer.addView(bloodTypeView);
        }
    }


    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDate : endDate;
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            String formattedDate = dateFormat.format(calendar.getTime());
            if (isStartDate) {
                startDateBtn.setText(formattedDate);
            } else {
                endDateBtn.setText(formattedDate);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(boolean isStartTime) {
        LocalTime currentTime = isStartTime ? startTime : endTime;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            LocalTime selectedTime = LocalTime.of(hourOfDay, minute);
            if (isStartTime) {
                startTime = selectedTime;
                startTimeBtn.setText(selectedTime.toString());
            } else {
                endTime = selectedTime;
                endTimeBtn.setText(selectedTime.toString());
            }
        }, currentTime.getHour(), currentTime.getMinute(), false).show();
    }

    private void saveChanges() {
        try {
            // Get current targets and maintain them unless explicitly changed
            Map<String, Double> bloodTypeTargets = new HashMap<>();
            Set<String> newOrIncreasedTargets = new HashSet<>();

            // First, get all current targets
            for (BloodTypeProgress progress : eventDetails.getBloodProgress()) {
                bloodTypeTargets.put(progress.getBloodType(), progress.getTargetAmount());
            }

            // Then update only the ones that have been changed
            for (Map.Entry<String, EditText> entry : bloodTypeInputs.entrySet()) {
                String bloodType = entry.getKey();
                String value = entry.getValue().getText().toString().trim();

                if (!value.isEmpty()) {
                    double newTarget = Double.parseDouble(value);
                    double currentTarget = bloodTypeTargets.getOrDefault(bloodType, 0.0);

                    // If target is new or increased
                    if (newTarget > currentTarget) {
                        newOrIncreasedTargets.add(bloodType);
                    }

                    // Update target
                    bloodTypeTargets.put(bloodType, newTarget);
                }
                // If value is empty, keep the existing target (don't remove it)
            }

            UpdateEventDTO updateDto = new UpdateEventDTO(eventDetails);
            updateDto.setTitle(titleInput.getText().toString().trim());
            updateDto.setDescription(descriptionInput.getText().toString().trim());
            updateDto.setStartTime(startDate.getTimeInMillis());
            updateDto.setEndTime(endDate.getTimeInMillis());
            updateDto.setAddress(address);
            updateDto.setLatitude(latitude);
            updateDto.setLongitude(longitude);
            updateDto.setBloodTypeTargets(bloodTypeTargets);  // This won't affect collected amounts
            updateDto.setDonationStartTime(startTime);
            updateDto.setDonationEndTime(endTime);

            ApiResponse<DonationEvent> response = ServiceLocator.getEventService()
                    .updateEvent(eventId, updateDto);

            if (response.isSuccess()) {
                // Notify only for new or increased targets
                if (!newOrIncreasedTargets.isEmpty()) {
                    notifyBloodTypeNeeded(eventId, updateDto.getTitle(), newOrIncreasedTargets);
                }

                Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid blood type target values", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyBloodTypeNeeded(String eventId, String eventTitle, Set<String> bloodTypes) {
        NotificationManager notificationManager = new NotificationManager(
                this, ServiceLocator.getDatabaseHelper());

        try {
            // Get all donor users
            List<User> donors = ServiceLocator.getUserRepository()
                    .findUsersByTimeRange(0, System.currentTimeMillis())  // Get all users
                    .stream()
                    .filter(user -> user.getUserType() == UserType.DONOR &&
                            user.getBloodType() != null &&
                            bloodTypes.contains(user.getBloodType()))
                    .collect(Collectors.toList());

            for (User donor : donors) {
                notificationManager.createEventNotification(
                        donor.getUserId(),
                        eventId,
                        "New Blood Type Needed",
                        String.format("Event '%s' now needs your blood type: %s",
                                eventTitle, donor.getBloodType()),
                        NotificationType.BLOOD_TYPE_MATCH
                );
            }
        } catch (AppException e) {
            Log.e("EditEventActivity", "Error notifying donors", e);
        }
    }
}