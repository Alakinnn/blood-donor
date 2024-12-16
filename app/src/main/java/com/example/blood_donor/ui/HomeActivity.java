package com.example.blood_donor.ui;


import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.blood_donor.R;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.FunFactAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
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

        // Implement endless scrolling
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

        // Load initial events
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
            // Handle navigation
            return true;
        });
    }

    private void loadUserName() {
        String userName = AuthManager.getInstance().getUserName();
        TextView headerText = findViewById(R.id.headerText);
        headerText.setText(getString(R.string.hello_user, userName));
    }

    private void loadMoreEvents(int page) {
        // Implement pagination loading
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