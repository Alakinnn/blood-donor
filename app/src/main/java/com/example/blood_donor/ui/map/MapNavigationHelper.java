package com.example.blood_donor.ui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

public class MapNavigationHelper {
    public static void navigateToLocation(Context context, LatLng destination, String name) {
        @SuppressLint("DefaultLocale") String uri = String.format("google.navigation:q=%f,%f&mode=d",
                destination.latitude,
                destination.longitude);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        }
    }
}