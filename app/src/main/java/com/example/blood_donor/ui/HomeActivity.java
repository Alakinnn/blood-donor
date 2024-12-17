package com.example.blood_donor.ui;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.FunFactAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private ViewPager2 funFactsCarousel;
    private RecyclerView eventList;
    private TextInputLayout searchLayout;
    private View filterContainer;
    private BottomNavigationView bottomNavigation;
    private com.example.blood_donor.ui.adapters.EventAdapter eventAdapter;
    private boolean isManager;

    private static final int PAGE_SIZE = 20; // Remove the import for MifareUltralight.PAGE_SIZE
    private boolean isLoading = false;
    private final EventService eventService;

    public HomeActivity() {
        this.eventService = ServiceLocator.getEventService(); // Replace static import
    }

    private final List<String> funFacts = Arrays.asList(
            "One donation can save up to three lives",
            "Blood can't be manufactured – it can only come from donors",
            "A person needs blood every two seconds"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        isManager = AuthManager.getInstance().getUserType() == UserType.SITE_MANAGER;

        initializeViews();
        setupFunFactsCarousel();
        setupEventList();
        setupSearch();
        setupBottomNavigation();
        loadUserName();
    }

    private void initializeViews() {
        funFactsCarousel = findViewById(R.id.funFactsCarousel);
        eventList = findViewById(R.id.eventList);
        searchLayout = findViewById(R.id.searchLayout);
        filterContainer = findViewById(R.id.filterContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);

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
        eventList.setLayoutManager(new LinearLayoutManager(this));
        eventList.setAdapter(eventAdapter);

        eventAdapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            startActivity(intent);
        });

        EndlessRecyclerViewScrollListener scrollListener =
                new EndlessRecyclerViewScrollListener(
                        (LinearLayoutManager) eventList.getLayoutManager()
                ) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount) {
                        loadMoreEvents(page);
                    }
                };
        eventList.addOnScrollListener(scrollListener);

        loadMoreEvents(0);
    }

    private void setupSearch() {
        // Implement search functionality
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // First clear and set the appropriate menu
        bottomNavigation.getMenu().clear();
        UserType userType = AuthManager.getInstance().getUserType();
        bottomNavigation.inflateMenu(userType == UserType.SITE_MANAGER ?
                R.menu.bottom_nav_manager : R.menu.bottom_nav_donor);

        // Then set up selection
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_map) {
                Intent intent = new Intent(this, MapActivity.class);
                // Add transition animation
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (itemId == R.id.nav_history) {
                // TODO: Implement history navigation
                return true;
            } else if (itemId == R.id.nav_profile) {
                // TODO: Implement profile navigation
                return true;
            } else if (itemId == R.id.nav_create && isManager) {
                // TODO: Implement event creation for managers
                return true;
            } else if (itemId == R.id.nav_my_events && isManager) {
                // TODO: Implement my events for managers
                return true;
            }
            return false;
        });
    }

    private void loadUserName() {
        String userName = AuthManager.getInstance().getUserName();
        TextView headerText = findViewById(R.id.headerText);
        headerText.setText(getString(R.string.hello_user, userName));
    }

    private void loadMoreEvents(int page) {
        if (isLoading) return;
        isLoading = true;

        // Create query
        EventQueryDTO query = new EventQueryDTO(
                null, // latitude
                null, // longitude
                null, // zoomLevel
                searchLayout.getEditText().getText().toString(), // searchTerm
                null, // bloodTypes - implement filter later
                "date", // sortBy
                "desc", // sortOrder
                page + 1, // page number
                PAGE_SIZE // pageSize
        );

        // Load events
        ApiResponse<List<EventSummaryDTO>> response = eventService.getEventSummaries(query);
        if (response.isSuccess() && response.getData() != null) {
            runOnUiThread(() -> {
                eventAdapter.addEvents(response.getData());
                isLoading = false;
            });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
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