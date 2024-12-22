package com.example.blood_donor.ui.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.blood_donor.R;
import com.example.blood_donor.ui.map.EventClusterItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private final View window;
    private final Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        window = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getInfoWindow(Marker marker) {
        EventClusterItem event = (EventClusterItem) marker.getTag();
        if (event == null) return null;

        TextView titleText = window.findViewById(R.id.title);
        TextView snippetText = window.findViewById(R.id.snippet);
        TextView progressText = window.findViewById(R.id.progress);
        TextView statusText = window.findViewById(R.id.event_status);
        if (event.getStatus() != null) {
            statusText.setText("Status: " + event.getStatus());
            statusText.setVisibility(View.VISIBLE);
        }
        titleText.setText(event.getTitle());
        snippetText.setText(event.getSnippet());
        progressText.setText(String.format("%.1f%% Complete",
                (event.getCurrentBloodCollected() / event.getBloodGoal()) * 100));

        return window;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
