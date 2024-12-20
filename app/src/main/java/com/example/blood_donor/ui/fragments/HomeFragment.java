package com.example.blood_donor.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.FunFactAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {
    private ViewPager2 funFactsCarousel;
    private RecyclerView eventList;
    private TextInputLayout searchLayout;
    private View filterContainer;
    private EventAdapter eventAdapter;
    private boolean isLoading = false;
    private final EventService eventService;
    private static final int PAGE_SIZE = 20;
    private ChipGroup bloodTypeFilter;
    private String currentSearchTerm;
    private List<String> selectedBloodTypes = new ArrayList<>();

    private final List<String> funFacts = Arrays.asList(
            "One donation can save up to three lives",
            "Blood can't be manufactured – it can only come from donors",
            "A person needs blood every two seconds"
    );

    public HomeFragment() {
        this.eventService = ServiceLocator.getEventService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupFunFactsCarousel();
        setupEventList();
        setupSearch();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserName();
    }

    private void initializeViews(View view) {
        funFactsCarousel = view.findViewById(R.id.funFactsCarousel);
        eventList = view.findViewById(R.id.eventList);
        searchLayout = view.findViewById(R.id.searchLayout);
        filterContainer = view.findViewById(R.id.filterContainer);
        bloodTypeFilter = view.findViewById(R.id.bloodTypeFilter);

        searchLayout.setEndIconOnClickListener(v ->
                filterContainer.setVisibility(
                        filterContainer.getVisibility() == View.VISIBLE ?
                                View.GONE : View.VISIBLE
                )
        );
    }

    private void setupFunFactsCarousel() {
        FunFactAdapter adapter = new FunFactAdapter(funFacts);
        funFactsCarousel.setAdapter(adapter);
    }

    private void setupEventList() {
        eventAdapter = new EventAdapter();
        eventList.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList.setAdapter(eventAdapter);

        eventAdapter.setOnEventClickListener(event -> {
            // Store the event data in cache before navigation
            ServiceLocator.getEventService().cacheEventDetails(event);

            Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            startActivity(intent);
        });


        eventList.addOnScrollListener(new EndlessRecyclerViewScrollListener(
                (LinearLayoutManager) eventList.getLayoutManager()) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                loadMoreEvents(page);
            }
        });

        loadMoreEvents(0);
    }

    private void setupSearch() {
        searchLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchTerm = s.toString();
                // Debounce search with a handler
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        setupBloodTypeFilter();
    }

    private final Handler searchHandler = new Handler();
    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            refreshEvents();
        }
    };

    private void setupBloodTypeFilter() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String bloodType : bloodTypes) {
            Chip chip = new Chip(requireContext());
            chip.setText(bloodType);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBloodTypes.add(bloodType);
                } else {
                    selectedBloodTypes.remove(bloodType);
                }
                // Refresh events immediately when selection changes
                refreshEvents();
            });
            bloodTypeFilter.addView(chip);
        }
    }

    private void refreshEvents() {
        eventAdapter.clearEvents();
        loadMoreEvents(0);
    }


    private void loadUserName() {
        String userName = AuthManager.getInstance().getUserName();
        // Use findViewById on the View obtained from onCreateView
        TextView headerText = requireView().findViewById(R.id.headerText);
        headerText.setText(getString(R.string.hello_user, userName));
    }

    private void loadMoreEvents(int page) {
        if (isLoading) return;
        isLoading = true;

        EventQueryDTO query = new EventQueryDTO(
                null, // latitude
                null, // longitude
                null, // zoomLevel
                searchLayout.getEditText().getText().toString(), // searchTerm
                selectedBloodTypes.isEmpty() ? null : selectedBloodTypes, // Add blood type filter
                "date", // sortBy
                "desc", // sortOrder
                page + 1, // page number
                PAGE_SIZE // pageSize
        );

        ApiResponse<List<EventSummaryDTO>> response = eventService.getEventSummaries(query);
        if (response.isSuccess() && response.getData() != null) {
            requireActivity().runOnUiThread(() -> {
                eventAdapter.addEvents(response.getData());
                isLoading = false;
            });
        } else {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                isLoading = false;
            });
        }
    }

    private static abstract class EndlessRecyclerViewScrollListener
            extends RecyclerView.OnScrollListener {
        private int currentPage = 0;
        private final LinearLayoutManager layoutManager;

        public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager) {
            this.layoutManager = layoutManager;
        }

        @Override
        public void onScrolled(RecyclerView view, int dx, int dy) {
            int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            int totalItemCount = layoutManager.getItemCount();

            if (lastVisibleItem + 5 >= totalItemCount) {
                currentPage++;
                onLoadMore(currentPage, totalItemCount);
            }
        }

        public abstract void onLoadMore(int page, int totalItemsCount);
    }
}