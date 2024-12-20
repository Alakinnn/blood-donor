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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class HistoryFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private View managerControls;
    private AutoCompleteTextView reportFormatSpinner;
    private MaterialButton generateReportButton;
    private MaterialButton showStatsButton;
    private boolean isManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        isManager = AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER;

        initializeViews(view);
        setupViewPager();
        if (isManager) {
            setupManagerControls();
        }
        return view;
    }

    private void initializeViews(View view) {
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView);
        managerControls = view.findViewById(R.id.managerControls);

        if (isManager) {
            reportFormatSpinner = view.findViewById(R.id.reportFormatSpinner);
            generateReportButton = view.findViewById(R.id.generateReportButton);
            showStatsButton = view.findViewById(R.id.showStatsButton);
            managerControls.setVisibility(View.VISIBLE);
        } else {
            managerControls.setVisibility(View.GONE);
        }
    }

    private void setupViewPager() {
        HistoryPagerAdapter pagerAdapter = new HistoryPagerAdapter(this, isManager);
        viewPager.setAdapter(pagerAdapter);

        // Set up tab titles
        String[] titles = isManager ?
                new String[]{"Joined Events", "My Events"} :
                new String[]{"My Events"};

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(titles[position])).attach();

        // Listen for tab changes to show/hide manager controls
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (isManager) {
                    managerControls.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void setupManagerControls() {
        // Setup report format spinner
        String[] formats = {"CSV", "PDF", "EXCEL"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                formats
        );
        reportFormatSpinner.setAdapter(adapter);

        // Generate report button click handler
        generateReportButton.setOnClickListener(v -> {
            String selectedFormat = reportFormatSpinner.getText().toString();
            String managerId = AuthManager.getInstance().getUserId();

            try {
                ReportFormat format = ReportFormat.valueOf(selectedFormat);
                byte[] report = ServiceLocator.getAnalyticsService()
                        .generateSiteManagerReport(managerId, format);

                // Handle the generated report (save to file or share)
                // Implementation would depend on your file handling strategy
                Toast.makeText(requireContext(),
                        "Report generated successfully",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Error generating report: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Show statistics button click handler
        showStatsButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EventStatisticsActivity.class);
            intent.putExtra("managerId", AuthManager.getInstance().getUserId());
            startActivity(intent);
        });
    }
}

// ViewPager adapter for the history tabs
class HistoryPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Fragment fragment;
    private final boolean isManager;
    private final int itemCount;

    public HistoryPagerAdapter(Fragment fragment, boolean isManager) {
        this.fragment = fragment;
        this.isManager = isManager;
        this.itemCount = isManager ? 2 : 1;
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
}

class HistoryViewHolder extends RecyclerView.ViewHolder {
    final RecyclerView recyclerView;

    HistoryViewHolder(View view) {
        super(view);
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
    }
}