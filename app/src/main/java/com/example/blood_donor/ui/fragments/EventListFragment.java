package com.example.blood_donor.ui.fragments;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.event.DonationEvent;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.EditEventActivity;
import com.example.blood_donor.ui.EventDetailsActivity;
import com.example.blood_donor.ui.EventStatisticsActivity;
import com.example.blood_donor.ui.adapters.EventAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends Fragment {
    private RecyclerView recyclerView;
    private EventAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_history_page, container, false);
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        String type = getArguments().getString("type");
        loadEvents(type);

        return view;
    }

    private void loadEvents(String type) {
        String userId = AuthManager.getInstance().getUserId();
        try {
            List<EventSummaryDTO> events;

            switch (type) {
                case "all_events":
                    EventQueryDTO query = new EventQueryDTO(
                            null, null, null, null, null,
                            "date", "desc", 1, 50, null, null
                    );
                    ApiResponse<List<DonationEvent>> response =
                            ServiceLocator.getSuperUserEventService().findAllEvents(query);
                    if (response.isSuccess()) {
                        ApiResponse<List<EventSummaryDTO>> summaryResponse =
                                ServiceLocator.getEventService().convertToEventSummaries(response.getData());
                        events = summaryResponse.getData();
                    } else {
                        throw new AppException(response.getErrorCode(), response.getMessage());
                    }
                    break;

                case "joined_events":
                    events = ServiceLocator.getEventRepository().findJoinedEvents(userId);
                    break;

                case "managed_events":
                    events = ServiceLocator.getEventRepository().findManagedEvents(userId);
                    break;

                default:
                    events = new ArrayList<>();
                    break;
            }

            setupAdapter(events, type);

        } catch (AppException e) {
            Toast.makeText(requireContext(),
                    "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAdapter(List<EventSummaryDTO> events, String type) {
        adapter = new EventAdapter();

        // Set up adapter properties based on type
        if (type.equals("all_events") || type.equals("managed_events")) {
            adapter.setShowManagerActions(true);
        }

        // Set up click listeners
        adapter.setOnEventClickListener((event, sharedElement) -> {
            Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());

            // Set up shared element transition
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    sharedElement, // The shared element passed from the ViewHolder
                    "event_card_transition" // Must match the transitionName in the layout
            );

            startActivity(intent, options.toBundle());
        });


        // Set up action listeners if needed
        if (type.equals("all_events") || type.equals("managed_events")) {
            setupActionListeners(adapter);
        }

        recyclerView.setAdapter(adapter);
        adapter.addEvents(events);
    }

    private void setupActionListeners(EventAdapter adapter) {
        adapter.setOnEventActionListener(new EventAdapter.OnEventActionListener() {
            @Override
            public void onStatisticsClick(EventSummaryDTO event) {
                Intent intent = new Intent(requireContext(), EventStatisticsActivity.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            }

            @Override
            public void onReportClick(EventSummaryDTO event) {
                ((HistoryFragment) requireParentFragment()).showReportFormatDialog(event.getEventId());
            }

            @Override
            public void onEditClick(EventSummaryDTO event) {
                Intent intent = new Intent(requireContext(), EditEventActivity.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            }
        });

        // Add cancel button for super user
        if (AuthManager.getInstance().getUserType() == UserType.SUPER_USER) {
            adapter.setOnCancelClickListener((event, sharedElement) -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Cancel Event")
                        .setMessage("Are you sure you want to cancel this event?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            ApiResponse<Void> response =
                                    ServiceLocator.getSuperUserEventService().cancelEvent(event.getEventId());
                            if (response.isSuccess()) {
                                Toast.makeText(requireContext(),
                                        "Event cancelled",
                                        Toast.LENGTH_SHORT).show();
                                loadEvents("all_events"); // Reload the list
                            } else {
                                Toast.makeText(requireContext(),
                                        response.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }
}
