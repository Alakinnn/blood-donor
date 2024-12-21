package com.example.blood_donor.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.notifications.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<NotificationItem> notifications = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String notificationId);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void removeNotification(int position) {
        notifications.remove(position);
        notifyItemRemoved(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView messageView;
        private final TextView timeView;
        private final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.notificationTitle);
            messageView = itemView.findViewById(R.id.notificationMessage);
            timeView = itemView.findViewById(R.id.notificationTime);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(NotificationItem item) {
            titleView.setText(item.getTitle());
            messageView.setText(item.getMessage());
            timeView.setText(formatTime(item.getCreatedAt()));

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(item.getNotificationId());
                    removeNotification(getAdapterPosition());
                }
            });
        }

        private String formatTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < TimeUnit.MINUTES.toMillis(1)) {
                return "Just now";
            } else if (diff < TimeUnit.HOURS.toMillis(1)) {
                return diff / TimeUnit.MINUTES.toMillis(1) + "m ago";
            } else if (diff < TimeUnit.DAYS.toMillis(1)) {
                return diff / TimeUnit.HOURS.toMillis(1) + "h ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}

