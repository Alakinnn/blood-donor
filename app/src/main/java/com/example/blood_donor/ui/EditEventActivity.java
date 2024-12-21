package com.example.blood_donor.ui;

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
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.models.response.ApiResponse;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Override
    public void onBackPressed() {
        // Show confirmation dialog if there are unsaved changes
        super.onBackPressed();
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
        // Compare current values with original values
        boolean titleChanged = !titleInput.getText().toString().equals(eventDetails.getTitle());
        boolean descriptionChanged = !descriptionInput.getText().toString().equals(eventDetails.getDescription());
        boolean addressChanged = !locationInput.getText().toString().equals(eventDetails.getAddress());

        // Compare dates and times
        boolean dateTimeChanged = startDate.getTimeInMillis() != eventDetails.getStartTime() ||
                endDate.getTimeInMillis() != eventDetails.getEndTime();

        // Compare blood type targets
        boolean bloodTypeTargetsChanged = false;
        for (Map.Entry<String, EditText> entry : bloodTypeInputs.entrySet()) {
            String currentValue = entry.getValue().getText().toString();
            double originalValue = 0.0;
            for (BloodTypeProgress progress : eventDetails.getBloodProgress()) {
                if (progress.getBloodType().equals(entry.getKey())) {
                    originalValue = progress.getTargetAmount();
                    break;
                }
            }
            if (!currentValue.isEmpty() && Double.parseDouble(currentValue) != originalValue) {
                bloodTypeTargetsChanged = true;
                break;
            }
        }

        return titleChanged || descriptionChanged || addressChanged ||
                dateTimeChanged || bloodTypeTargetsChanged;
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        startDateBtn = findViewById(R.id.startDateBtn);
        endDateBtn = findViewById(R.id.endDateBtn);
        startTimeBtn = findViewById(R.id.startTimeBtn);
        endTimeBtn = findViewById(R.id.endTimeBtn);
        locationInput = findViewById(R.id.locationInput);
        bloodTypeContainer = findViewById(R.id.bloodTypeContainer);

        findViewById(R.id.saveButton).setOnClickListener(v -> saveChanges());
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

        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String bloodType : bloodTypes) {
            View bloodTypeView = inflater.inflate(R.layout.blood_type_input_item, bloodTypeContainer, false);
            TextInputLayout inputLayout = bloodTypeView.findViewById(R.id.amountInputLayout);
            TextInputEditText amountInput = bloodTypeView.findViewById(R.id.amountInput);

            inputLayout.setHint(bloodType + " Target (L)");
            amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            // Set current value if exists
            if (eventDetails != null && eventDetails.getBloodProgress() != null) {
                for (BloodTypeProgress progress : eventDetails.getBloodProgress()) {
                    if (bloodType.equals(progress.getBloodType())) {
                        amountInput.setText(String.format(Locale.getDefault(), "%.1f",
                                progress.getTargetAmount()));
                        break;
                    }
                }
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
            // Validate inputs
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String address = locationInput.getText().toString().trim();

            if (title.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Title and location are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get coordinates from address
            List<Address> addresses = new Geocoder(this).getFromLocationName(address, 1);
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Invalid address", Toast.LENGTH_SHORT).show();
                return;
            }

            UpdateEventDTO updateDto = new UpdateEventDTO();
            updateDto.setTitle(title);
            updateDto.setDescription(description);
            updateDto.setStartTime(startDate.getTimeInMillis());
            updateDto.setEndTime(endDate.getTimeInMillis());
            updateDto.setAddress(address);
            updateDto.setLatitude(addresses.get(0).getLatitude());
            updateDto.setLongitude(addresses.get(0).getLongitude());
            updateDto.setDonationStartTime(startTime);
            updateDto.setDonationEndTime(endTime);

            // Collect blood type targets
            Map<String, Double> bloodTypeTargets = new HashMap<>();
            for (Map.Entry<String, EditText> entry : bloodTypeInputs.entrySet()) {
                String value = entry.getValue().getText().toString();
                if (!value.isEmpty()) {
                    bloodTypeTargets.put(entry.getKey(), Double.parseDouble(value));
                }
            }
            updateDto.setBloodTypeTargets(bloodTypeTargets);

            // Save changes
            ApiResponse<DonationEvent> response = ServiceLocator.getEventService()
                    .updateEvent(eventId, updateDto);
            if (response.isSuccess()) {
                notifyParticipants(eventId, updateDto.getTitle());

                Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error validating address", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid blood type target values", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyParticipants(String eventId, String eventTitle) {
        try {
            // Get all registrations for this event
            List<Registration> registrations = ServiceLocator.getRegistrationRepository()
                    .getEventRegistrations(eventId);

            // Extract unique user IDs
            List<String> userIds = registrations.stream()
                    .map(Registration::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            // Create notifications
            NotificationManager notificationManager = new NotificationManager(
                    this, ServiceLocator.getDatabaseHelper());
            notificationManager.notifyEventUpdate(eventId, eventTitle, userIds);

            // Reschedule reminders if date/time changed
            for (String userId : userIds) {
                notificationManager.scheduleEventReminders(
                        userId,
                        eventId,
                        eventTitle,
                        eventDetails.getStartTime()
                );
            }
        } catch (AppException e) {
            Log.e("EditEventActivity", "Error notifying participants", e);
        }
    }
}