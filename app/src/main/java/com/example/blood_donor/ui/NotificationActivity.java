package com.example.blood_donor.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blood_donor.R;
import com.example.blood_donor.server.notifications.NotificationItem;
import com.example.blood_donor.ui.adapters.NotificationAdapter;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.NotificationManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.transition.platform.MaterialContainerTransform;

import java.util.List;

public class NotificationActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private NotificationManager notificationManager;
    private MaterialButton clearAllButton;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        userId = AuthManager.getInstance().getUserId();
        notificationManager = new NotificationManager(this, ServiceLocator.getDatabaseHelper());

        initializeViews();
        setupRecyclerView();
        loadNotifications();

        // Mark all as read when opened
        notificationManager.markAllAsRead(userId);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.notificationsList);
        clearAllButton = findViewById(R.id.clearAllButton);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        clearAllButton.setOnClickListener(v -> {
            notificationManager.deleteAllNotifications(userId);
            adapter.clearNotifications();
            Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnDeleteClickListener(notificationId -> {
            notificationManager.deleteNotification(notificationId);
            loadNotifications(); // Reload the list after deletion
        });
    }

    private void loadNotifications() {
        List<NotificationItem> notifications = notificationManager.getUnreadNotifications(userId);
        adapter.setNotifications(notifications);

        // Show/hide the empty state
        View emptyState = findViewById(R.id.emptyState);
        if (notifications.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            clearAllButton.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            clearAllButton.setVisibility(View.VISIBLE);
        }
    }
}