package com.example.blood_donor.ui.fragments;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
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

    private MaterialTextView eventTitleView;
    private MaterialTextView eventDateView;
    private MaterialTextView eventProgressView;
    private EventMarkerDTO selectedEvent;

    public MapFragment() {
        this.eventService = ServiceLocator.getEventService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        initializeViews(view);
        setupLocationClient();
        setupMap();
        return view;
    }

    private void initializeViews(View view) {
        bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        eventTitleView = view.findViewById(R.id.event_title);
        eventDateView = view.findViewById(R.id.event_date);
        eventProgressView = view.findViewById(R.id.event_progress);

        view.findViewById(R.id.view_details_button).setOnClickListener(v -> {
            if (selectedEvent != null) {
                navigateToEventDetails(selectedEvent.getEventId());
            }
        });
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        try {
            map = googleMap;

            // Set up UI settings
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setRotateGesturesEnabled(false);

            // Handle map events
            map.setOnCameraIdleListener(() -> {
                try {
                    loadEventsInView();
                } catch (Exception e) {
                    Log.e(TAG, "Error loading events", e);
                }
            });

            map.setOnMarkerClickListener(marker -> {
                try {
                    return onMarkerClick(marker);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling marker click", e);
                    return false;
                }
            });

            // Disable right click/long press
            map.setOnMapLongClickListener(null);

            checkLocationPermission();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up map", e);
        }
    }

    private void loadEventsInView() {
        if (map == null) return;
        try {
            LatLng center = map.getCameraPosition().target;
            float zoomLevel = map.getCameraPosition().zoom;

            // Only load events if we're zoomed in enough
            if (zoomLevel < 10) {
                clearMarkers();
                return;
            }

            EventQueryDTO query = new EventQueryDTO(
                    center.latitude,
                    center.longitude,
                    (double) zoomLevel,
                    null,
                    null,
                    "distance",
                    "asc",
                    1,
                    50
            );

            ApiResponse<List<EventMarkerDTO>> response = eventService.getEventMarkers(query);
            if (response.isSuccess() && response.getData() != null) {
                updateMarkers(response.getData());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading events", e);
        }
    }

    private void clearMarkers() {
        if (map != null) {
            map.clear();
            markerEventMap.clear();
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
    }

    private void navigateToEventDetails(String eventId) {
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