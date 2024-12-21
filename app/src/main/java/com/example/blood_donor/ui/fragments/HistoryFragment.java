package com.example.blood_donor.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.utils.FileUtils;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.EventStatisticsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.server.models.modules.ReportFormat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class HistoryFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private boolean isManager;
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private String pendingEventId;
    private String pendingFormat;


    public interface EventActionCallback {
        void showReportFormatDialog(String eventId);
        void onStatisticsClick(String eventId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        isManager = AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER;

        initializeViews(view);
        setupViewPager();
        return view;
    }

    private void initializeViews(View view) {
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
    }

    private void showReportFormatDialog(String eventId) {
        String[] formats = {"CSV", "PDF", "EXCEL"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Report Format")
                .setItems(formats, (dialog, which) -> {
                    String format = formats[which];
                    checkStoragePermissionAndGenerateReport(eventId, format);
                })
                .show();
    }

    private void generateReport(String eventId, String format) {
        try {
            ReportFormat reportFormat = ReportFormat.valueOf(format);
            byte[] report = ServiceLocator.getAnalyticsService()
                    .generateEventReport(eventId, reportFormat);

            // Save and share the report
            FileUtils.saveAndShareReport(requireContext(), report, eventId, format);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Error generating report: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void checkStoragePermissionAndGenerateReport(String eventId, String format) {
        // For Android 10 (API level 29) and above, we don't need storage permission for app-specific files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateReport(eventId, format);
            return;
        }

        // For Android 6.0 (API level 23) to Android 9.0 (API level 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Save pending operation details
                pendingEventId = eventId;
                pendingFormat = format;

                // Show permission rationale if needed
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Permission Required")
                            .setMessage("Storage permission is needed to save the report file.")
                            .setPositiveButton("Grant Permission", (dialog, which) -> {
                                // Request the permission
                                requestPermissions(
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        STORAGE_PERMISSION_CODE
                                );
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // Request the permission directly
                    requestPermissions(
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STORAGE_PERMISSION_CODE
                    );
                }
            } else {
                // Permission already granted
                generateReport(eventId, format);
            }
        } else {
            // For Android 5.1 (API level 22) and below, permissions are granted at install time
            generateReport(eventId, format);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            // Check if permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with pending operation
                if (pendingEventId != null && pendingFormat != null) {
                    generateReport(pendingEventId, pendingFormat);
                }
            } else {
                // Permission denied
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // User clicked "Never ask again", show settings dialog
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Permission Required")
                            .setMessage("Storage permission is needed to save reports. Please grant the permission in Settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(requireContext(),
                            "Storage permission is required to save reports",
                            Toast.LENGTH_LONG).show();
                }
            }
            // Clear pending operation
            pendingEventId = null;
            pendingFormat = null;
        }
    }

    private void setupViewPager() {
        HistoryPagerAdapter pagerAdapter = new HistoryPagerAdapter(this, isManager,
                new EventActionCallback() {
                    @Override
                    public void showReportFormatDialog(String eventId) {
                        HistoryFragment.this.showReportFormatDialog(eventId);
                    }

                    @Override
                    public void onStatisticsClick(String eventId) {
                        Intent intent = new Intent(requireContext(), EventStatisticsActivity.class);
                        intent.putExtra("eventId", eventId);
                        startActivity(intent);
                    }
                });
        viewPager.setAdapter(pagerAdapter);

        // Set up tab titles
        String[] titles = isManager ?
                new String[]{"Joined Events", "My Events"} :
                new String[]{"My Events"};

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(titles[position])).attach();
    }
}

// ViewPager adapter for the history tabs
class HistoryPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Fragment fragment;
    private final boolean isManager;
    private final int itemCount;
    private final HistoryFragment.EventActionCallback callback;

    public HistoryPagerAdapter(Fragment fragment, boolean isManager, HistoryFragment.EventActionCallback callback) {
        this.fragment = fragment;
        this.isManager = isManager;
        this.itemCount = isManager ? 2 : 1;
        this.callback = callback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_history_page, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryViewHolder historyHolder = (HistoryViewHolder) holder;
        if (position == 0) {
            loadJoinedEvents(historyHolder.recyclerView);
        } else {
            loadManagedEvents(historyHolder.recyclerView);
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    private void loadJoinedEvents(RecyclerView recyclerView) {
        String userId = AuthManager.getInstance().getUserId();
        try {
            List<EventSummaryDTO> events = ServiceLocator.getEventRepository()
                    .findJoinedEvents(userId);

            EventAdapter adapter = new EventAdapter();
            adapter.setOnEventClickListener(event -> {
                Intent intent = new Intent(fragment.requireContext(),
                        EventDetailsActivity.class);
                intent.putExtra("eventId", event.getEventId());
                fragment.startActivity(intent);
            });

            recyclerView.setAdapter(adapter);
            adapter.addEvents(events);
        } catch (AppException e) {
            Toast.makeText(recyclerView.getContext(),
                    "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadManagedEvents(RecyclerView recyclerView) {
        String userId = AuthManager.getInstance().getUserId();
        try {
            List<EventSummaryDTO> events = ServiceLocator.getEventRepository()
                    .findManagedEvents(userId);

            EventAdapter adapter = new EventAdapter();
            adapter.setShowManagerActions(true);
            adapter.setOnEventClickListener(event -> {
                Intent intent = new Intent(fragment.requireContext(),
                        EventDetailsActivity.class);
                intent.putExtra("eventId", event.getEventId());
                fragment.startActivity(intent);
            });

            adapter.setOnEventActionListener(new EventAdapter.OnEventActionListener() {
                @Override
                public void onStatisticsClick(EventSummaryDTO event) {
                    callback.onStatisticsClick(event.getEventId());
                }

                @Override
                public void onReportClick(EventSummaryDTO event) {
                    callback.showReportFormatDialog(event.getEventId());
                }
            });

            recyclerView.setAdapter(adapter);
            adapter.addEvents(events);
        } catch (AppException e) {
            Toast.makeText(recyclerView.getContext(),
                    "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}

class HistoryViewHolder extends RecyclerView.ViewHolder {
    final RecyclerView recyclerView;

    HistoryViewHolder(View view) {
        super(view);
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
    }
}