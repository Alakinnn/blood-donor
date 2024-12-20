package com.example.blood_donor.ui.fragments;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapSearchHandler {
    private final Context context;
    private final GoogleMap map;
    private final TextInputLayout searchLayout;
    private final EditText searchInput;
    private final ChipGroup bloodTypeFilter;
    private final View bloodTypeScroll;
    private final SearchListener searchListener;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<String> selectedBloodTypes = new ArrayList<>();
    private final Geocoder geocoder;

    public interface SearchListener {
        void onSearch(EventQueryDTO query);
        void onSearchCleared();
    }

    public MapSearchHandler(Context context, GoogleMap map, TextInputLayout searchLayout,
                            ChipGroup bloodTypeFilter, View bloodTypeScroll, SearchListener listener) {
        this.context = context;
        this.map = map;
        this.searchLayout = searchLayout;
        this.searchInput = searchLayout.getEditText();
        this.bloodTypeFilter = bloodTypeFilter;
        this.bloodTypeScroll = bloodTypeScroll;
        this.searchListener = listener;
        this.geocoder = new Geocoder(context);

        setup();
    }

    private void setup() {
        // Setup search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    searchListener.onSearchCleared();
                } else {
                    searchHandler.removeCallbacksAndMessages(null);
                    searchHandler.postDelayed(() -> performSearch(s.toString()), 300);
                }
            }
        });

        // Setup filter toggle
        searchLayout.setEndIconOnClickListener(v -> {
            bloodTypeScroll.setVisibility(
                    bloodTypeScroll.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        setupBloodTypeFilter();
    }

    private void setupBloodTypeFilter() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String bloodType : bloodTypes) {
            Chip chip = new Chip(context);
            chip.setText(bloodType);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBloodTypes.add(bloodType);
                } else {
                    selectedBloodTypes.remove(bloodType);
                }
                if (searchInput.length() > 0) {
                    performSearch(searchInput.getText().toString());
                }
            });
            bloodTypeFilter.addView(chip);
        }
    }

    void performSearch(String query) {
        try {
            LatLng searchLocation = null;
            // Try to geocode the query first
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                searchLocation = new LatLng(address.getLatitude(), address.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLocation, 15));
            }

            // Create query DTO
            LatLng center = searchLocation != null ? searchLocation : map.getCameraPosition().target;
            EventQueryDTO queryDTO = new EventQueryDTO(
                    center.latitude,
                    center.longitude,
                    (double) map.getCameraPosition().zoom,
                    query,
                    selectedBloodTypes.isEmpty() ? null : selectedBloodTypes,
                    "distance",
                    "asc",
                    1,
                    50
            );

            searchListener.onSearch(queryDTO);

        } catch (IOException e) {
            e.printStackTrace();
            // Fall back to text-only search
            performTextSearch(query);
        }
    }

    private void performTextSearch(String query) {
        LatLng center = map.getCameraPosition().target;
        EventQueryDTO queryDTO = new EventQueryDTO(
                center.latitude,
                center.longitude,
                (double) map.getCameraPosition().zoom,
                query,
                selectedBloodTypes.isEmpty() ? null : selectedBloodTypes,
                "distance",
                "asc",
                1,
                50
        );
        searchListener.onSearch(queryDTO);
    }
}