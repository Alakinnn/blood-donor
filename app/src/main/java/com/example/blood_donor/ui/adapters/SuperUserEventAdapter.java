package com.example.blood_donor.ui.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.models.event.EventStatus;
import com.google.android.material.button.MaterialButton;

public class SuperUserEventAdapter extends EventAdapter {
    private OnEventActionListener actionListener;

    public interface OnEventActionListener extends EventAdapter.OnEventActionListener {
        void onCancelClick(EventSummaryDTO event);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        EventSummaryDTO event = getEvent(position);

        // Add status indicator
        TextView statusText = holder.itemView.findViewById(R.id.event_status);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Status: " + event.getStatus());

        // Show all actions for super user
        ViewGroup actionsContainer = holder.itemView.findViewById(R.id.managerActions);
        actionsContainer.setVisibility(View.VISIBLE);

        // Add cancel button if event is not already cancelled
        if (event.getStatus() != EventStatus.CANCELLED) {
            MaterialButton cancelButton = new MaterialButton(holder.itemView.getContext());
            cancelButton.setText("Cancel Event");
            cancelButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    ((OnEventActionListener) actionListener).onCancelClick(event);
                }
            });
            actionsContainer.addView(cancelButton);
        }
    }

    @Override
    public void setOnEventActionListener(EventAdapter.OnEventActionListener listener) {
        if (listener instanceof OnEventActionListener) {
            this.actionListener = (OnEventActionListener) listener;
        }
        super.setOnEventActionListener(listener);
    }
}