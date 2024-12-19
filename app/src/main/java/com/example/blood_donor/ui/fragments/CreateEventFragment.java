package com.example.blood_donor.ui.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.CreateEventDTO;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEventFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap map;
    private MaterialCardView mapPreviewCard;
    private TextView locationText;
    private LatLng selectedLocation;
    private String selectedAddress;
    private boolean isMapExpanded = false;
    private MaterialButton startTimeBtn, endTimeBtn, startDateBtn, endDateBtn;
    private long startDate, endDate;
    private LocalTime startTime, endTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);
        view.findViewById(R.id.createButton).setOnClickListener(v -> handleEventCreation());
        initializeViews(view);
        setupMap();
        setupTimeSelectors(view);
        setupDateSelectors(view);
        setupBloodTypeInputs(view);

        return view;
    }


    private void initializeViews(View view) {
        mapPreviewCard = view.findViewById(R.id.mapPreviewCard);
        locationText = view.findViewById(R.id.locationText);

        mapPreviewCard.setOnClickListener(v -> toggleMapExpansion());
    }

    private void setupTimeSelectors(View view) {
        startTimeBtn = view.findViewById(R.id.startTimeButton);
        endTimeBtn = view.findViewById(R.id.endTimeButton);

        startTimeBtn.setOnClickListener(v -> showTimePickerDialog(true));
        endTimeBtn.setOnClickListener(v -> showTimePickerDialog(false));
    }

    private void showTimePickerDialog(boolean isStartTime) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    if (isStartTime) {
                        startTimeBtn.setText("Start: " + time);
                        startTime = LocalTime.of(hourOfDay, minute);
                    } else {
                        endTimeBtn.setText("End: " + time);
                        endTime = LocalTime.of(hourOfDay, minute);
                    }
                },
                12, 0, false
        );
        timePickerDialog.show();
    }


    private void toggleMapExpansion() {
        ViewGroup.LayoutParams params = mapPreviewCard.getLayoutParams();

        if (isMapExpanded) {
            // Collapse map
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
        } else {
            // Expand map
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 600, getResources().getDisplayMetrics());
        }
        mapPreviewCard.setLayoutParams(params);
        isMapExpanded = !isMapExpanded;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            getAddressFromLocation(latLng);
            map.clear();
            map.addMarker(new MarkerOptions().position(latLng));
            toggleMapExpansion(); // Collapse map after selection
        });
    }

    private void getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                selectedAddress = address.getAddressLine(0);
                locationText.setText(selectedAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupDateSelectors(View view) {
        startDateBtn = view.findViewById(R.id.startDateButton);
        endDateBtn = view.findViewById(R.id.endDateButton);

        startDateBtn.setOnClickListener(v -> showDatePickerDialog(true));
        endDateBtn.setOnClickListener(v -> showDatePickerDialog(false));
    }

    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);

                    if (isStartDate) {
                        startDate = selectedCalendar.getTimeInMillis();
                        startDateBtn.setText("Start: " + formatDate(startDate));
                    } else {
                        endDate = selectedCalendar.getTimeInMillis();
                        endDateBtn.setText("End: " + formatDate(endDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private String formatDate(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timeInMillis));
    }

    private void setupBloodTypeInputs(View view) {
        LinearLayout container = view.findViewById(R.id.bloodTypeContainer);
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        for (String bloodType : bloodTypes) {
            View bloodTypeView = getLayoutInflater().inflate(
                    R.layout.blood_type_input_item, container, false);

            TextView typeLabel = bloodTypeView.findViewById(R.id.bloodTypeLabel);
            TextInputEditText amountInput = bloodTypeView.findViewById(R.id.amountInput);

            typeLabel.setText(bloodType);
            container.addView(bloodTypeView);
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    private void handleEventCreation() {
        // Get all form inputs
        String title = ((TextInputEditText) requireView().findViewById(R.id.titleInput)).getText().toString();
        String description = ((TextInputEditText) requireView().findViewById(R.id.descriptionInput)).getText().toString();

        // Basic field validation
        if (title.isEmpty() || description.isEmpty() || selectedLocation == null ||
                startDate == 0 || endDate == 0 || startTime == null || endTime == null) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Date validation
        if (endDate < startDate) {
            Toast.makeText(getContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }

        // If same date, check times
        if (endDate == startDate && endTime.isBefore(startTime)) {
            Toast.makeText(getContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create timestamps by combining dates and times
        LocalDateTime startDateTime = LocalDateTime.of(
                Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
                startTime
        );
        LocalDateTime endDateTime = LocalDateTime.of(
                Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
                endTime
        );

        if (endDateTime.isBefore(startDateTime)) {
            Toast.makeText(getContext(), "Event end must be after event start", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect blood type requirements
        Map<String, Double> bloodTypeTargets = new HashMap<>();
        LinearLayout container = requireView().findViewById(R.id.bloodTypeContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView typeLabel = child.findViewById(R.id.bloodTypeLabel);
            TextInputEditText amountInput = child.findViewById(R.id.amountInput);
            String bloodType = typeLabel.getText().toString();
            String amount = amountInput.getText().toString().trim();

            if (!amount.isEmpty()) {
                try {
                    double value = Double.parseDouble(amount);
                    if (value > 0) {
                        bloodTypeTargets.put(bloodType, value);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(),
                            "Please enter valid numbers for blood amounts",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        CreateEventDTO eventDTO = new CreateEventDTO(
                title,
                description,
                startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                getTotalBloodGoal(bloodTypeTargets),
                selectedAddress,
                selectedLocation.latitude,
                selectedLocation.longitude,
                "",  // Location description
                startTime,
                endTime
        );
        eventDTO.setBloodTypeTargets(bloodTypeTargets);

        String userId = AuthManager.getInstance().getUserId();
        ApiResponse<DonationEvent> response = ServiceLocator.getEventService()
                .createEvent(userId, eventDTO);

        if (response.isSuccess()) {
            Toast.makeText(getContext(),
                    "Event created successfully",
                    Toast.LENGTH_SHORT).show();
            navigateToMap(response.getData());
        } else {
            Toast.makeText(getContext(),
                    response.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private double getTotalBloodGoal(Map<String, Double> bloodTypeTargets) {
        return bloodTypeTargets.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
    private void navigateToMap(DonationEvent event) {
        // Navigate to MapFragment
        MapFragment mapFragment = new MapFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mapFragment)
                .commit();
    }
}