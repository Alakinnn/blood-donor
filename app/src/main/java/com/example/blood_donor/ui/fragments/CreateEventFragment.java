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

import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
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
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                    } else {
                        endTimeBtn.setText("End: " + time);
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
}