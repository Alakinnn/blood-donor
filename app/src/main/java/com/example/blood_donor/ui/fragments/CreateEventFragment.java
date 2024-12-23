package com.example.blood_donor.ui.fragments;

import android.annotation.SuppressLint;
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
import com.example.blood_donor.server.models.user.UserType;
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
import com.google.android.material.textfield.TextInputLayout;

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
import java.util.Objects;

public class CreateEventFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap map;
    private MaterialCardView mapPreviewCard;
    private TextView locationText;
    private LatLng selectedLocation;
    private String selectedAddress;
    private boolean isMapExpanded = false;
    private MaterialButton startTimeBtn, endTimeBtn, startDateBtn, endDateBtn;
    private long startDate, endDate;
    private long selectedStartDate;
    private long selectedEndDate;
    private LocalTime startTime, endTime;
    private TextInputLayout managerEmailLayout;
    private TextInputEditText managerEmailInput;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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
        managerEmailLayout = view.findViewById(R.id.managerEmailLayout);
        managerEmailInput = view.findViewById(R.id.managerEmailInput);
        mapPreviewCard.setOnClickListener(v -> toggleMapExpansion());

        if (AuthManager.getInstance().getUserType() == UserType.SUPER_USER) {
            managerEmailLayout.setVisibility(View.VISIBLE);
        } else {
            managerEmailLayout.setVisibility(View.GONE);
        }
    }

    private void setupTimeSelectors(View view) {
        startTimeBtn = view.findViewById(R.id.startTimeButton);
        endTimeBtn = view.findViewById(R.id.endTimeButton);

        startTimeBtn.setOnClickListener(v -> showTimePickerDialog(true));
        endTimeBtn.setOnClickListener(v -> showTimePickerDialog(false));
    }

    private void showTimePickerDialog(boolean isStartTime) {
        @SuppressLint("SetTextI18n") TimePickerDialog timePickerDialog = new TimePickerDialog(
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
            assert addresses != null;
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
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_MONTH, 3); // Add 3 days to current date
        minDate.set(Calendar.HOUR_OF_DAY, 0);
        minDate.set(Calendar.MINUTE, 0);
        minDate.set(Calendar.SECOND, 0);
        minDate.set(Calendar.MILLISECOND, 0);

        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate > 0) {
            calendar.setTimeInMillis(selectedStartDate);
        } else if (!isStartDate && selectedEndDate > 0) {
            calendar.setTimeInMillis(selectedEndDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedCal.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCal.set(Calendar.MINUTE, 0);
                    selectedCal.set(Calendar.SECOND, 0);
                    selectedCal.set(Calendar.MILLISECOND, 0);

                    // Check if selected date is at least 3 days from now
                    if (selectedCal.before(minDate)) {
                        Toast.makeText(requireContext(),
                                "Event must be at least 3 days from today",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validate end date comes after start date
                    if (!isStartDate && selectedStartDate > 0) {
                        if (selectedCal.getTimeInMillis() < selectedStartDate) {
                            Toast.makeText(requireContext(),
                                    "End date must be after start date",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if (isStartDate) {
                        selectedStartDate = selectedCal.getTimeInMillis();
                        startDate = selectedStartDate; // Set the value used in validation
                        startDateBtn.setText(dateFormat.format(selectedCal.getTime()));
                    } else {
                        selectedEndDate = selectedCal.getTimeInMillis();
                        endDate = selectedEndDate; // Set the value used in validation
                        endDateBtn.setText(dateFormat.format(selectedCal.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date constraints
        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        } else {
            // For end date, minimum should be either 3 days from now or the start date, whichever is later
            long minEndDate = Math.max(minDate.getTimeInMillis(), selectedStartDate);
            datePickerDialog.getDatePicker().setMinDate(minEndDate);
        }

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
        String title = Objects.requireNonNull(((TextInputEditText) requireView().findViewById(R.id.titleInput)).getText()).toString();
        String description = Objects.requireNonNull(((TextInputEditText) requireView().findViewById(R.id.descriptionInput)).getText()).toString();

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
            String amount = Objects.requireNonNull(amountInput.getText()).toString().trim();

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

        if (AuthManager.getInstance().getUserType() == UserType.SUPER_USER) {
            String managerEmail = Objects.requireNonNull(managerEmailInput.getText()).toString().trim();
            if (managerEmail.isEmpty()) {
                Toast.makeText(getContext(), "Manager email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiResponse<DonationEvent> response = ServiceLocator.getSuperUserEventService()
                    .createEventForManager(managerEmail, eventDTO);
            handleCreateResponse(response);
        } else {
            String userId = AuthManager.getInstance().getUserId();
            ApiResponse<DonationEvent> response = ServiceLocator.getEventService()
                    .createEvent(userId, eventDTO);
            handleCreateResponse(response);
        }
    }

    private double getTotalBloodGoal(Map<String, Double> bloodTypeTargets) {
        return bloodTypeTargets.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private void handleCreateResponse(ApiResponse<DonationEvent> response) {
        if (response.isSuccess()) {
            Toast.makeText(getContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
            navigateToMap(response.getData());
        } else {
            Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
        }
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