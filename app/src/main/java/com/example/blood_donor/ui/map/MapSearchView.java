package com.example.blood_donor.ui.map;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.SearchView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

public class MapSearchView extends SearchView {
    private final GoogleMap map;
    private final Geocoder geocoder;

    public MapSearchView(Context context, GoogleMap map) {
        super(context);
        this.map = map;
        this.geocoder = new Geocoder(context);

        setupSearchListener();
    }

    private void setupSearchListener() {
        setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void searchLocation(String query) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
