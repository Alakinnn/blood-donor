package com.example.blood_donor.ui.adapters;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
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
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnEventClickListener {
        void onEventClick(EventSummaryDTO event);
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
        private final TextView dateTimeView;
        private final LinearProgressIndicator progressBar;
        private final TextView progressText;
        private final TextView bloodTypesView;
        private final MaterialButton detailsButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.eventTitle);
            dateTimeView = itemView.findViewById(R.id.eventDateTime);
            progressBar = itemView.findViewById(R.id.bloodProgress);
            progressText = itemView.findViewById(R.id.progressText);
            detailsButton = itemView.findViewById(R.id.detailsButton);
            bloodTypesView = itemView.findViewById(R.id.requiredBloodTypes);
        }

        void bind(EventSummaryDTO event) {
            titleView.setText(event.getTitle());
            dateTimeView.setText(String.format("%s - %s",
                    dateFormat.format(new Date(event.getStartTime())),
                    dateFormat.format(new Date(event.getEndTime()))
            ));

            // Set up progress
            int progress = (int) ((event.getCurrentBloodCollected() / event.getBloodGoal()) * 100);
            progressBar.setProgress(progress);
            progressText.setText(String.format(Locale.getDefault(),
                    "%d%% of %.1fL goal", progress, event.getBloodGoal()));

            detailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(events.get(getAdapterPosition()));
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
        }
    }
}

