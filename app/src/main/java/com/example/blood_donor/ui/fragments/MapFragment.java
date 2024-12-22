package com.example.blood_donor.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.ui.map.MapSearchView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private MaterialCardView bottomSheet;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;
    private final EventService eventService;
    private final Map<Marker, EventMarkerDTO> markerEventMap = new HashMap<>();
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private List<String> selectedBloodTypes = new ArrayList<>();

    private MaterialTextView eventTitleView;
    private MaterialTextView eventDateView;
    private MaterialTextView eventProgressView;
    private EventMarkerDTO selectedEvent;
    private MaterialButton directionsButton;
    private TextInputLayout searchLayout;
    private ChipGroup bloodTypeFilter;
    private View bloodTypeScroll;
    private MapSearchHandler searchHandler;
    private View rootView;
    private MaterialButton startDateButton;
    private MaterialButton endDateButton;
    private MaterialButton clearDatesButton;
    private Long selectedStartDate;
    private Long selectedEndDate;
    private View dateFilterContainer;

    public MapFragment() {
        this.eventService = ServiceLocator.getEventService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        initializeViews(rootView);
        setupLocationClient();
        setupMap();
        return rootView;
    }

    private void initializeViews(View view) {
        bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        eventTitleView = view.findViewById(R.id.event_title);
        eventDateView = view.findViewById(R.id.event_date);
        eventProgressView = view.findViewById(R.id.event_progress);
        directionsButton = view.findViewById(R.id.directions_button);
        searchLayout = view.findViewById(R.id.search_layout);
        bloodTypeFilter = view.findViewById(R.id.bloodTypeFilter);
        bloodTypeScroll = view.findViewById(R.id.blood_type_scroll);
        // Initialize date filter views
        dateFilterContainer = view.findViewById(R.id.dateFilterContainer);
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        clearDatesButton = view.findViewById(R.id.clearDatesButton);

        // Setup click listeners
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        clearDatesButton.setOnClickListener(v -> clearDates());

        // Modify your existing filter toggle to show/hide date filter
        searchLayout.setEndIconOnClickListener(v -> {
            boolean isVisible = bloodTypeScroll.getVisibility() == View.VISIBLE;
            bloodTypeScroll.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            dateFilterContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        // Setup blood type chips
        setupBloodTypeFilter(view.findViewById(R.id.bloodTypeFilter));

        view.findViewById(R.id.view_details_button).setOnClickListener(v -> {
            if (selectedEvent != null) {
                navigateToEventDetails(selectedEvent.getEventId());
            }
        });

        directionsButton.setOnClickListener(v -> {
            if (selectedEvent != null) {
                openGoogleMapsNavigation(selectedEvent);
            }
        });
    }

    private void initializeDateFilter(View view) {
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        clearDatesButton = view.findViewById(R.id.clearDatesButton);

        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        clearDatesButton.setOnClickListener(v -> clearDates());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate != null) {
            calendar.setTimeInMillis(selectedStartDate);
        } else if (!isStartDate && selectedEndDate != null) {
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

                    if (isStartDate) {
                        selectedStartDate = selectedCal.getTimeInMillis();
                        startDateButton.setText(dateFormat.format(selectedCal.getTime()));
                    } else {
                        selectedEndDate = selectedCal.getTimeInMillis();
                        endDateButton.setText(dateFormat.format(selectedCal.getTime()));
                    }
                    refreshEvents();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set min/max dates if needed
        if (!isStartDate && selectedStartDate != null) {
            datePickerDialog.getDatePicker().setMinDate(selectedStartDate);
        } else if (isStartDate && selectedEndDate != null) {
            datePickerDialog.getDatePicker().setMaxDate(selectedEndDate);
        }

        datePickerDialog.show();
    }

    private void clearDates() {
        selectedStartDate = null;
        selectedEndDate = null;
        startDateButton.setText("Start Date");
        endDateButton.setText("End Date");
        refreshEvents();
    }

    private void setupBloodTypeFilter(ChipGroup bloodTypeFilter) {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        for (String bloodType : bloodTypes) {
            Chip chip = new Chip(requireContext());
            chip.setText(bloodType);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBloodTypes.add(bloodType);
                } else {
                    selectedBloodTypes.remove(bloodType);
                }
                refreshEvents();
            });
            bloodTypeFilter.addView(chip);
        }
    }

    private void executeSearch(EventQueryDTO query) {
        ApiResponse<List<EventMarkerDTO>> response = eventService.getEventMarkers(query);
        if (response.isSuccess() && response.getData() != null) {
            updateMarkers(response.getData());
            if (!response.getData().isEmpty()) {
                fitBoundsToMarkers(response.getData());
            }
        }
    }

    private void fitBoundsToMarkers(List<EventMarkerDTO> events) {
        if (events.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (EventMarkerDTO event : events) {
            builder.include(new LatLng(event.getLatitude(), event.getLongitude()));
        }

        try {
            int padding = getResources().getDimensionPixelSize(R.dimen.map_padding);
            LatLngBounds bounds = builder.build();

            if (rootView != null) {
                rootView.post(() -> {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                });
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Error fitting bounds", e);
        }
    }

    private void openGoogleMapsNavigation(EventMarkerDTO event) {
        // Create a Uri for Google Maps navigation
        Uri gmmIntentUri = Uri.parse(String.format(Locale.US,
                "google.navigation:q=%f,%f",
                event.getLatitude(),
                event.getLongitude()));

        // Create an Intent to open Google Maps
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(requireContext(),
                    "Google Maps is not installed",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnCameraIdleListener(this::loadEventsInView);
        map.setOnMarkerClickListener(this::onMarkerClick);
        searchHandler = new MapSearchHandler(
                requireContext(),
                map,
                searchLayout,
                bloodTypeFilter,
                bloodTypeScroll,
                dateFilterContainer,  // Add the date filter container
                new MapSearchHandler.SearchListener() {
                    @Override
                    public void onSearch(EventQueryDTO query) {
                        executeSearch(query);
                    }

                    @Override
                    public void onSearchCleared() {
                        loadEventsInView();
                    }
                }
        );

        checkLocationPermission();
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        clearDatesButton.setOnClickListener(v -> {
            clearDates();
            searchHandler.clearDateFilters();
        });

    }

    private void refreshEvents() {
        if (map == null) return;

        LatLng center = map.getCameraPosition().target;
        EventQueryDTO query = new EventQueryDTO(
                center.latitude,
                center.longitude,
                (double) map.getCameraPosition().zoom,
                searchLayout.getEditText().getText().toString(),
                selectedBloodTypes.isEmpty() ? null : selectedBloodTypes,
                "distance",
                "asc",
                1,
                50,
                selectedStartDate,
                selectedEndDate
        );
        executeSearch(query);
    }

    private void loadEventsInView() {
        if (map == null) return;

        LatLng center = map.getCameraPosition().target;
        float zoomLevel = map.getCameraPosition().zoom;

        EventQueryDTO query = new EventQueryDTO(
                center.latitude,
                center.longitude,
                (double) zoomLevel,
                null,
                null,
                "distance",
                "asc",
                1,
                50,
                null,
                null
        );

        ApiResponse<List<EventMarkerDTO>> response;
        if (AuthManager.getInstance().getUserType() == UserType.SUPER_USER) {
            // Super users can see all events
            List<DonationEvent> allEvents = ServiceLocator.getSuperUserEventService()
                    .findAllEvents(query).getData();
            response = ServiceLocator.getEventService().getEventMarkers(query);
        } else {
            // Regular users only see visible events
            response = ServiceLocator.getEventService().getEventMarkers(query);
        }

        if (response.isSuccess() && response.getData() != null) {
            updateMarkers(response.getData());
        }
    }

    private void updateMarkers(List<EventMarkerDTO> events) {
        markerEventMap.clear();
        map.clear();

        for (EventMarkerDTO event : events) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(event.getLatitude(), event.getLongitude()))
                    .title(event.getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            getMarkerColor(event.getCurrentBloodCollected(),
                                    event.getBloodGoal())));

            Marker marker = map.addMarker(markerOptions);
            if (marker != null) {
                markerEventMap.put(marker, event);
            }
        }
    }

    private float getMarkerColor(double collected, double goal) {
        double progress = (collected / goal) * 100;
        if (progress >= 90) return BitmapDescriptorFactory.HUE_GREEN;
        if (progress >= 50) return BitmapDescriptorFactory.HUE_YELLOW;
        return BitmapDescriptorFactory.HUE_RED;
    }

    private boolean onMarkerClick(Marker marker) {
        EventMarkerDTO event = markerEventMap.get(marker);
        if (event == null) return false;

        selectedEvent = event;
        updateBottomSheet(event);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        return true;
    }

    private void updateBottomSheet(EventMarkerDTO event) {
        eventTitleView.setText(event.getTitle());
        eventDateView.setText(String.format("%s - %s",
                dateFormat.format(new Date(event.getStartTime())),
                dateFormat.format(new Date(event.getEndTime()))));

        double progress = (event.getCurrentBloodCollected() / event.getBloodGoal()) * 100;
        eventProgressView.setText(String.format(Locale.getDefault(),
                "%.1f%% Complete (%.1f/%.1f L)",
                progress,
                event.getCurrentBloodCollected(),
                event.getBloodGoal()));

        TextView bloodTypesView = bottomSheet.findViewById(R.id.required_blood_types);
        List<String> bloodTypes = event.getRequiredBloodTypes();
        if (bloodTypes != null && !bloodTypes.isEmpty()) {
            bloodTypesView.setText("Blood Types Needed: " + String.join(", ", bloodTypes));
        } else {
            bloodTypesView.setText("Blood Types Needed: None specified");
        }
    }

    private void navigateToEventDetails(String eventId) {
        Log.d("MapFragment", "Opening event details with ID: " + eventId);

        // Get event data from markerEventMap using selected event
        EventMarkerDTO marker = selectedEvent;
        if (marker != null) {
            // Create and cache event summary before navigation
            EventSummaryDTO summary = new EventSummaryDTO();
            summary.setEventId(marker.getEventId());
            summary.setTitle(marker.getTitle());
            summary.setAddress(marker.getAddress());
            summary.setStartTime(marker.getStartTime());
            summary.setEndTime(marker.getEndTime());
            summary.setRequiredBloodTypes(marker.getRequiredBloodTypes());
            summary.setBloodGoal(marker.getBloodGoal());
            summary.setCurrentBloodCollected(marker.getCurrentBloodCollected());
            summary.setLatitude(marker.getLatitude());
            summary.setLongitude(marker.getLongitude());

            // Cache the event details
            ServiceLocator.getEventService().cacheEventDetails(summary);
        }

        Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                loadEventsInView();
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}