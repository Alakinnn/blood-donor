package com.example.blood_donor.ui.map;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.SearchView;

import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapSearchView extends SearchView {
    private final GoogleMap map;
    private final Geocoder geocoder;
    private final SearchListener searchListener;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable;
    private List<String> selectedBloodTypes = new ArrayList<>();
    private ChipGroup bloodTypeFilter;
    private Long selectedStartDate;
    private Long selectedEndDate;

    public interface SearchListener {
        void onSearch(EventQueryDTO query);
        void onSearchCleared();
    }

    public MapSearchView(Context context, GoogleMap map, SearchListener listener, ChipGroup bloodTypeFilter) {
        super(context);
        this.map = map;
        this.geocoder = new Geocoder(context);
        this.searchListener = listener;
        this.bloodTypeFilter = bloodTypeFilter;

        searchRunnable = () -> performSearch(getQuery().toString());
        setupSearchListener();
    }

    private void setupSearchListener() {
        setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                clearFocus(); // Hide keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    searchListener.onSearchCleared();
                    return true;
                }

                // Debounce search
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        setOnCloseListener(() -> {
            searchListener.onSearchCleared();
            return false;
        });
    }

    private void performSearch(String query) {
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
                    50,
                    selectedStartDate,   // Add date filters
                    selectedEndDate
            );

            searchListener.onSearch(queryDTO);

        } catch (IOException e) {
            e.printStackTrace();
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
                50,
                selectedStartDate,   // Add date filters
                selectedEndDate
        );
        searchListener.onSearch(queryDTO);
    }

    public void setSelectedBloodTypes(List<String> bloodTypes) {
        this.selectedBloodTypes = bloodTypes;
        if (!TextUtils.isEmpty(getQuery())) {
            performSearch(getQuery().toString());
        }
    }

    // Add methods for date handling
    public void setDateFilters(Long startDate, Long endDate) {
        this.selectedStartDate = startDate;
        this.selectedEndDate = endDate;
        if (!TextUtils.isEmpty(getQuery())) {
            performSearch(getQuery().toString());
        }
    }

    public void clearDateFilters() {
        this.selectedStartDate = null;
        this.selectedEndDate = null;
        if (!TextUtils.isEmpty(getQuery())) {
            performSearch(getQuery().toString());
        }
    }
}