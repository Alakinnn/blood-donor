package com.example.blood_donor.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blood_donor.R;
import com.example.blood_donor.server.models.notification.Notification;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;

import java.util.List;

public class NotificationListFragment extends Fragment {
//    private RecyclerView notificationList;
//    private NotificationAdapter adapter;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
//
//        notificationList = view.findViewById(R.id.notification_list);
//        setupRecyclerView();
//
//        // Add swipe to delete
//        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
//            @Override
//            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//                return false;
//            }
//
//            @Override
//            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
//                Notification notification = adapter.getItem(viewHolder.getAdapterPosition());
//                deleteNotification(notification.getNotificationId());
//            }
//        };
//
//        new ItemTouchHelper(swipeCallback).attachToRecyclerView(notificationList);
//
//        return view;
//    }
//
//    private void loadNotifications() {
//        String userId = AuthManager.getInstance().getUserId();
//        List<Notification> notifications = ServiceLocator.getNotificationRepository()
//                .getUnreadNotifications(userId);
//        adapter.setNotifications(notifications);
//    }
}