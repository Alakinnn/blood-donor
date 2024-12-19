package com.example.blood_donor.ui.map;

import android.content.Context;

import com.example.blood_donor.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class MapClusterRenderer extends DefaultClusterRenderer<EventClusterItem> {
    public MapClusterRenderer(Context context, GoogleMap map,
                              ClusterManager<EventClusterItem> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(EventClusterItem item, MarkerOptions markerOptions) {
        markerOptions.icon(getMarkerIcon(item));
        markerOptions.title(item.getTitle());
    }

    private BitmapDescriptor getMarkerIcon(EventClusterItem item) {
        double progress = (item.getCurrentBloodCollected() / item.getBloodGoal()) * 100;
        if (progress >= 90) return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        if (progress >= 50) return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
    }
}
