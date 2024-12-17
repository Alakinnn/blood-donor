package com.example.blood_donor.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {
    private View rootView;
    private ViewPager2 funFactsCarousel;
    private RecyclerView eventList;
    private TextInputLayout searchLayout;
    private View filterContainer;
    private TextView headerText;
    private EventAdapter eventAdapter;
    private boolean isLoading = false;
    private final EventService eventService;
    private static final int PAGE_SIZE = 20;

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
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews();
        setupFunFactsCarousel();
        setupEventList();
        setupSearch();
        loadUserName();
    }

    private void initializeViews() {
        funFactsCarousel = rootView.findViewById(R.id.funFactsCarousel);
        eventList = rootView.findViewById(R.id.eventList);
        searchLayout = rootView.findViewById(R.id.searchLayout);
        filterContainer = rootView.findViewById(R.id.filterContainer);
        headerText = rootView.findViewById(R.id.headerText);

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
        // Implement search functionality
    }

    private void loadUserName() {
        String userName = AuthManager.getInstance().getUserName();
        if (headerText != null) {
            headerText.setText(getString(R.string.hello_user, userName));
        }
    }

    private void loadMoreEvents(int page) {
        if (isLoading) return;
        isLoading = true;

        EventQueryDTO query = new EventQueryDTO(
                null,
                null,
                null,
                searchLayout.getEditText().getText().toString(),
                null,
                "date",
                "desc",
                page + 1,
                PAGE_SIZE
        );

        ApiResponse<List<EventSummaryDTO>> response = eventService.getEventSummaries(query);
        if (response.isSuccess() && response.getData() != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                eventAdapter.addEvents(response.getData());
                isLoading = false;
            });
        } else if (isAdded()) {
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
        public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
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