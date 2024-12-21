package com.example.blood_donor.ui.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
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
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.notifications.NotificationItem;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.adapters.FunFactAdapter;
import com.example.blood_donor.ui.adapters.NotificationAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.server.services.EventService;
import com.example.blood_donor.ui.manager.NotificationManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    private Long selectedStartDate;
    private Long selectedEndDate;
    private MaterialButton startDateButton;
    private MaterialButton endDateButton;
    private MaterialButton clearDatesButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private NotificationManager notificationManager;
    private View notificationDot;
    private ImageButton notificationBell;
    private PopupWindow notificationPopup;
    private NotificationAdapter notificationAdapter;
    private Handler refreshHandler = new Handler();
    private static final long REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(1);


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
        setupNotificationSystem();
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
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        clearDatesButton = view.findViewById(R.id.clearDatesButton);
        notificationDot = view.findViewById(R.id.notificationDot);
        notificationBell = view.findViewById(R.id.notificationBell);

        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        clearDatesButton.setOnClickListener(v -> clearDates());

        searchLayout.setEndIconOnClickListener(v -> {
            boolean isVisible = filterContainer.getVisibility() == View.VISIBLE;
            filterContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.dateFilterContainer).setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });
        notificationBell.setOnClickListener(v -> showNotificationPopup(v));
    }

    private void setupNotificationSystem() {
        if (getContext() == null) return;

        notificationManager = new NotificationManager(
                requireContext(),
                ServiceLocator.getDatabaseHelper()
        );

        notificationManager.setNotificationCallback(hasUnread -> {
            if (notificationDot != null) {
                notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            }
        });

        // Initial check for notifications
        checkUnreadNotifications();

        // Start periodic checks
        startNotificationChecks();
    }

    private void setupNotificationUI() {
        ImageButton notificationBell = requireView().findViewById(R.id.notificationBell);
        View notificationDot = requireView().findViewById(R.id.notificationDot);

        notificationBell.setOnClickListener(v -> showNotificationPopup(v));

        // Initialize adapter
        notificationAdapter = new NotificationAdapter();
        notificationAdapter.setOnDeleteClickListener(notificationId -> {
            notificationManager.deleteNotification(notificationId);
            checkUnreadNotifications();
        });
    }

    private void checkUnreadNotifications() {
        if (notificationManager == null || !isAdded()) return;

        String userId = AuthManager.getInstance().getUserId();
        if (userId == null) return;

        // Check for blood type matches
        try {
            User currentUser = ServiceLocator.getUserRepository().findById(userId).orElse(null);
            if (currentUser != null && currentUser.getBloodType() != null) {
                notificationManager.checkBloodTypeMatchingEvents(userId, currentUser.getBloodType());
            }

            // Check unread status
            List<NotificationItem> unread = notificationManager.getUnreadNotifications(userId);
            if (notificationDot != null) {
                notificationDot.setVisibility(unread.isEmpty() ? View.GONE : View.VISIBLE);
            }
        } catch (AppException e) {
            Log.e("HomeFragment", "Error checking notifications", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (notificationManager != null) {
            checkUnreadNotifications();
        }
    }

    private void startNotificationChecks() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && notificationManager != null) {
                    checkUnreadNotifications();
                    handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1));
                }
            }
        }, TimeUnit.MINUTES.toMillis(1));
    }

    private void showNotificationPopup(View anchor) {
        if (notificationManager == null || !isAdded()) return;

        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
            return;
        }

        View popupView = getLayoutInflater().inflate(R.layout.popup_notifications, null);
        RecyclerView recyclerView = popupView.findViewById(R.id.notificationsList);
        MaterialButton clearAllButton = popupView.findViewById(R.id.clearAllButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        NotificationAdapter adapter = new NotificationAdapter();
        recyclerView.setAdapter(adapter);

        String userId = AuthManager.getInstance().getUserId();
        List<NotificationItem> notifications = notificationManager.getAllNotifications(userId);
        adapter.setNotifications(notifications);

        adapter.setOnDeleteClickListener(notificationId -> {
            notificationManager.deleteNotification(notificationId);
            // Refresh the list after deletion
            adapter.setNotifications(notificationManager.getAllNotifications(userId));
        });

        notificationPopup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        clearAllButton.setOnClickListener(v -> {
            notificationManager.deleteAllNotifications(userId);
            notificationPopup.dismiss();
        });

        // Mark notifications as read
        notificationManager.markAllAsRead(userId);

        // Apply background dim effect
        View container = new View(requireContext());
        container.setBackgroundColor(Color.parseColor("#80000000")); // Semi-transparent black
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Show popup with background dim
        PopupWindow dimPopup = new PopupWindow(container,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                false);
        dimPopup.showAtLocation(getView(), Gravity.NO_GRAVITY, 0, 0);

        notificationPopup.setOnDismissListener(() -> dimPopup.dismiss());
        notificationPopup.showAsDropDown(anchor);

        // Set elevation to show above dim background
        popupView.setElevation(16f);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate != null) {
            calendar.setTimeInMillis(selectedStartDate);
        } else if (!isStartDate && selectedEndDate != null) {
            calendar.setTimeInMillis(selectedEndDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedCal.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCal.set(Calendar.MINUTE, 0);
                    selectedCal.set(Calendar.SECOND, 0);

                    if (isStartDate) {
                        selectedStartDate = selectedCal.getTimeInMillis();
                        startDateButton.setText(dateFormat.format(selectedCal.getTime()));
                    } else {
                        selectedEndDate = selectedCal.getTimeInMillis();
                        endDateButton.setText(dateFormat.format(selectedCal.getTime()));
                    }
                    refreshEvents();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set min/max dates if needed
        if (!isStartDate && selectedStartDate != null) {
            datePickerDialog.getDatePicker().setMinDate(selectedStartDate);
        } else if (isStartDate && selectedEndDate != null) {
            datePickerDialog.getDatePicker().setMaxDate(selectedEndDate);
        }

        datePickerDialog.show();
    }

    private void clearDates() {
        selectedStartDate = null;
        selectedEndDate = null;
        startDateButton.setText("Start Date");
        endDateButton.setText("End Date");
        refreshEvents();
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
                PAGE_SIZE, // pageSize,
                selectedStartDate,
                selectedEndDate
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