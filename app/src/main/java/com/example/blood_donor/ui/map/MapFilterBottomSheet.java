package com.example.blood_donor.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.blood_donor.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

public class MapFilterBottomSheet extends BottomSheetDialogFragment {
    private FilterListener listener;
    private ChipGroup bloodTypeGroup;
    private RangeSlider dateRangeSlider;
    private RangeSlider distanceSlider;
    private ChipGroup statusGroup;

    public interface FilterListener {
        void onFiltersApplied(MapFilters filters);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_map_filters, container, false);
        setupViews(view);
        return view;
    }

    private void setupViews(View view) {
        // Initialize views and set up listeners
        // Implementation details omitted for brevity
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    public static class MapFilters {
        public List<String> bloodTypes = new ArrayList<>();
        public long startDate;
        public long endDate;
        public float distanceRadius;
        public List<String> eventStatuses = new ArrayList<>();
    }
}
