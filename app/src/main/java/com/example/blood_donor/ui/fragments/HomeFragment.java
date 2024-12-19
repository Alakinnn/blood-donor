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
import com.example.blood_donor.server.models.PagedResults;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.CacheService;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.FunFactAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {
    private ViewPager2 funFactsCarousel;
    private RecyclerView eventList;
    private TextInputLayout searchLayout;
    private View filterContainer;
    private EventAdapter eventAdapter;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    private final EventService eventService;
    private final CacheService cacheService;

    private final List<String> funFacts = Arrays.asList(
            "One donation can save up to three lives",
            "Blood can't be manufactured – it can only come from donors",
            "A person needs blood every two seconds"
    );

    public HomeFragment() {
        this.eventService = ServiceLocator.getEventService();
        this.cacheService = ServiceLocator.getCacheService();
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

        // Reset pagination state
        currentPage = 1;
        hasMoreData = true;
        eventAdapter.clearEvents();
        loadMoreEvents();
    }

    private void initializeViews(View view) {
        funFactsCarousel = view.findViewById(R.id.funFactsCarousel);
        eventList = view.findViewById(R.id.eventList);
        searchLayout = view.findViewById(R.id.searchLayout);
        filterContainer = view.findViewById(R.id.filterContainer);

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
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        eventList.setLayoutManager(layoutManager);
        eventList.setAdapter(eventAdapter);

        eventAdapter.setOnEventClickListener(event -> {
            // Let's add logging to debug
            Log.d("HomeFragment", "Clicked event ID: " + event.getEventId());

            // Cache event details before navigation
            ServiceLocator.getEventService().cacheEventDetails(event);

            Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            Log.d("HomeFragment", "Navigating to details with ID: " + event.getEventId());
            startActivity(intent);
        });

        // Add scroll listener for pagination
        eventList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                            && firstVisibleItemPosition >= 0) {
                        loadMoreEvents();
                    }
                }
            }
        });
    }

    private void loadUserName() {
        String userName = AuthManager.getInstance().getUserName();
        TextView headerText = requireView().findViewById(R.id.headerText);
        headerText.setText(getString(R.string.hello_user, userName));
    }

    private void loadMoreEvents() {
        if (isLoading) return;
        isLoading = true;

        if (currentPage == 1) {
            ServiceLocator.getCacheService().clear();
        }

        String cacheKey = "events_page_" + currentPage;
        PagedResults<EventSummaryDTO> cached = cacheService.get(cacheKey, PagedResults.class);
        if (cached != null) {
            handleEventResults(cached.getItems());
            return;
        }

        EventQueryDTO query = new EventQueryDTO(
                null,
                null,
                null,
                searchLayout.getEditText().getText().toString(),
                null,
                "date",
                "desc",
                currentPage,
                PAGE_SIZE
        );

        ApiResponse<PagedResults<EventSummaryDTO>> response = eventService.getEventSummaries(query);
        if (response.isSuccess() && response.getData() != null) {
            PagedResults<EventSummaryDTO> results = response.getData();
            cacheService.put(cacheKey, results, TimeUnit.MINUTES.toMillis(5));
            handleEventResults(results.getItems());
            hasMoreData = results.getTotalCount() > currentPage * PAGE_SIZE;
        } else {
            isLoading = false;
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void handleEventResults(List<EventSummaryDTO> events) {
        requireActivity().runOnUiThread(() -> {
            eventAdapter.addEvents(events);
            isLoading = false;
            hasMoreData = events.size() >= PAGE_SIZE;
            if (hasMoreData) {
                currentPage++;
            }
        });
    }

    private void setupSearch() {
        // Implement search functionality with debounce
        searchLayout.getEditText().addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            private Runnable runnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                runnable = () -> {
                    currentPage = 1;
                    hasMoreData = true;
                    eventAdapter.clearEvents();
                    loadMoreEvents();
                };
                handler.postDelayed(runnable, 300); // 300ms debounce delay
            }
        });
    }
}