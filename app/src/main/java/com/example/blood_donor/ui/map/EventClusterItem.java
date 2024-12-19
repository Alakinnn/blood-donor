package com.example.blood_donor.ui.map;

import androidx.annotation.Nullable;

import com.example.blood_donor.server.dto.events.EventMarkerDTO;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class EventClusterItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final String eventId;
    private final double bloodGoal;
    private final double currentBloodCollected;
    private final long startTime;
    private final long endTime;

    public EventClusterItem(EventMarkerDTO event) {
        this.position = new LatLng(event.getLatitude(), event.getLongitude());
        this.title = event.getTitle();
        this.snippet = event.getAddress();
        this.eventId = event.getEventId();
        this.bloodGoal = event.getBloodGoal();
        this.currentBloodCollected = event.getCurrentBloodCollected();
        this.startTime = event.getStartTime();
        this.endTime = event.getEndTime();
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

    public String getEventId() {
        return eventId;
    }

    public double getBloodGoal() {
        return bloodGoal;
    }

    public double getCurrentBloodCollected() {
        return currentBloodCollected;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}