package com.example.blood_donor.ui.adapters;


import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.EditEventActivity;
import com.example.blood_donor.ui.manager.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final List<EventSummaryDTO> events = new ArrayList<>();
    private OnEventClickListener listener;
    private OnEventClickListener cancelClickListener;
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private OnEventActionListener actionListener;
    private boolean showManagerActions = false;
    private boolean isHistoryView = false;

    public interface OnEventClickListener {
        void onEventClick(EventSummaryDTO event);
    }

    public void setIsHistoryView(boolean isHistoryView) {
        this.isHistoryView = isHistoryView;
    }

    public void setOnCancelClickListener(OnEventClickListener listener) {
        this.cancelClickListener = listener;
    }

    public interface OnEventActionListener {
        void onStatisticsClick(EventSummaryDTO event);
        void onReportClick(EventSummaryDTO event);
        void onEditClick(EventSummaryDTO event);
    }

    public void setShowManagerActions(boolean show) {
        this.showManagerActions = show;
    }

    public void setOnEventActionListener(OnEventActionListener listener) {
        this.actionListener = listener;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position));
        if (holder.managerActions != null) {
            holder.managerActions.setVisibility(
                    showManagerActions && isHistoryView ? View.VISIBLE : View.GONE);
        }
        if (cancelClickListener != null && holder.cancelButton != null) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v ->
                    cancelClickListener.onEventClick(events.get(position))
            );
        }
        if (showManagerActions) {
            holder.managerActions.setVisibility(View.VISIBLE);
            holder.editButton.setVisibility(View.VISIBLE);

            // Show cancel button only for super user
            if (AuthManager.getInstance().getUserType() == UserType.SUPER_USER) {
                holder.cancelButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
    public EventSummaryDTO getEvent(int position) {
        return events.get(position);
    }
    public void addEvents(List<EventSummaryDTO> newEvents) {
        for (EventSummaryDTO event : newEvents) {
            Log.d("EventAdapter", "Adding event: " + event.getEventId() + " - " + event.getTitle());
        }
        int startPosition = events.size();
        events.addAll(newEvents);
        notifyItemRangeInserted(startPosition, newEvents.size());
    }

    public void clearEvents() {
        events.clear();
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private View managerActions;
        private MaterialButton cancelButton;
        private MaterialButton statisticsButton;
        private MaterialButton reportButton;
        private final TextView dateTimeView;
        private final LinearProgressIndicator progressBar;
        private final TextView progressText;
        private final TextView bloodTypesView;
        private final MaterialButton detailsButton;
        private final ImageButton editButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.eventTitle);
            editButton = itemView.findViewById(R.id.editButton);
            dateTimeView = itemView.findViewById(R.id.eventDateTime);
            progressBar = itemView.findViewById(R.id.bloodProgress);
            progressText = itemView.findViewById(R.id.progressText);
            detailsButton = itemView.findViewById(R.id.detailsButton);
            bloodTypesView = itemView.findViewById(R.id.requiredBloodTypes);
            managerActions = itemView.findViewById(R.id.managerActions);
            statisticsButton = itemView.findViewById(R.id.statisticsButton);
            cancelButton = itemView.findViewById(R.id.cancelButton);
            reportButton = itemView.findViewById(R.id.reportButton);
        }

        void bind(EventSummaryDTO event) {
            titleView.setText(event.getTitle());
            dateTimeView.setText(String.format("%s - %s",
                    dateFormat.format(new Date(event.getStartTime())),
                    dateFormat.format(new Date(event.getEndTime()))
            ));

            // Set up progress
            double progress = (event.getBloodGoal() > 0) ?
                    (event.getCurrentBloodCollected() / event.getBloodGoal()) * 100 : 0;
            progress = Math.min(100, Math.max(0, progress));
            progressBar.setProgress((int) progress);
            progressText.setText(String.format(Locale.getDefault(),
                    "%.1f%% of %.1fL goal", progress, event.getBloodGoal()));
            if (showManagerActions) {
                editButton.setVisibility(View.VISIBLE);
                editButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onEditClick(event);
                    } else {
                        // Fallback direct navigation if no listener
                        Intent intent = new Intent(v.getContext(), EditEventActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        v.getContext().startActivity(intent);
                    }
                });
            } else {
                editButton.setVisibility(View.GONE);
            }

            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });

            List<String> bloodTypes = event.getRequiredBloodTypes();
            String bloodTypesText = "Blood Types Needed: " +
                    (bloodTypes != null && !bloodTypes.isEmpty() ?
                            String.join(", ", bloodTypes) : "None specified");
            bloodTypesView.setText(bloodTypesText);

            // Add donation hours if available
            if (event.getDonationStartTime() != null && event.getDonationEndTime() != null) {
                String donationHours = String.format("Donation Hours: %s - %s",
                        event.getDonationStartTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                        event.getDonationEndTime().format(DateTimeFormatter.ofPattern("h:mm a")));
                dateTimeView.append("\n" + donationHours);
            }

            managerActions.setVisibility(showManagerActions ? View.VISIBLE : View.GONE);
            if (showManagerActions && actionListener != null) {
                statisticsButton.setOnClickListener(v ->
                        actionListener.onStatisticsClick(event));
                reportButton.setOnClickListener(v ->
                        actionListener.onReportClick(event));
            }
        }
    }
}

