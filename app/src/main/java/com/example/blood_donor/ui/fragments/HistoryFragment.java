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
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.utils.FileUtils;
import com.example.blood_donor.ui.EditEventActivity;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.EventStatisticsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.SuperUserEventAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.example.blood_donor.server.models.modules.ReportFormat;
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
        // Check if user is manager OR super user
        isManager = AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER ||
                AuthManager.getInstance().getUserType() == UserType.SUPER_USER;

        initializeViews(view);
        setupViewPager();
        return view;
    }

    private void initializeViews(View view) {
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
    }

    void showReportFormatDialog(String eventId) {
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
        String[] titles;
        UserType userType = AuthManager.getInstance().getUserType();
        if (userType == UserType.SUPER_USER) {
            titles = new String[]{"All Events"};
        } else if (userType == UserType.SITE_MANAGER) {
            titles = new String[]{"Joined Events", "My Events"};
        } else {
            titles = new String[]{"My Events"};
        }

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(titles[position])).attach();
    }
}

// ViewPager adapter for the history tabs
class HistoryPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
    private final Fragment fragment;
    private final boolean isManager;
    private final HistoryFragment.EventActionCallback callback;
    private final UserType userType;

    public HistoryPagerAdapter(Fragment fragment, boolean isManager, HistoryFragment.EventActionCallback callback) {
        super(fragment);
        this.fragment = fragment;
        this.isManager = isManager;
        this.callback = callback;
        this.userType = AuthManager.getInstance().getUserType();
    }

    @Override
    public int getItemCount() {
        if (userType == UserType.SUPER_USER) {
            return 1; // Only "All Events" tab
        } else if (userType == UserType.SITE_MANAGER) {
            return 2; // "Joined Events" and "My Events" tabs
        } else {
            return 1; // "My Events" tab for donors
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        EventListFragment eventListFragment = new EventListFragment();
        Bundle args = new Bundle();

        if (userType == UserType.SUPER_USER) {
            args.putString("type", "all_events");
        } else if (userType == UserType.SITE_MANAGER) {
            args.putString("type", position == 0 ? "joined_events" : "managed_events");
        } else {
            args.putString("type", "joined_events");
        }

        eventListFragment.setArguments(args);
        return eventListFragment;
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