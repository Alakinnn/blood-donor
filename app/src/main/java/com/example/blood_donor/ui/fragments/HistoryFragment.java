package com.example.blood_donor.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.user.UserType;
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
                    generateReport(eventId, format);
                })
                .show();
    }

    private void generateReport(String eventId, String format) {
        try {
            ReportFormat reportFormat = ReportFormat.valueOf(format);
            byte[] report = ServiceLocator.getAnalyticsService()
                    .generateEventReport(eventId, reportFormat);

            Toast.makeText(requireContext(),
                    "Report generated successfully",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Error generating report: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
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