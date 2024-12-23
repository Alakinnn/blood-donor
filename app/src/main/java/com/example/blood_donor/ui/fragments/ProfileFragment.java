package com.example.blood_donor.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.notification.NotificationType;
import com.example.blood_donor.server.models.user.User;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.NotificationManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.stream.Collectors;

public class ProfileFragment extends Fragment {
    private MaterialTextView nameText;
    private MaterialTextView emailText;
    private MaterialTextView userTypeText;
    private MaterialTextView bloodTypeText;
    private MaterialTextView phoneText;
    private MaterialButton logoutButton;
    private View bloodTypeContainer;
    private View phoneContainer;
    private MaterialButton supportButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initializeViews(view);
        setupUserInfo();
        setupLogoutButton();
        return view;
    }

    private void initializeViews(View view) {
        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        userTypeText = view.findViewById(R.id.userTypeText);
        bloodTypeText = view.findViewById(R.id.bloodTypeText);
        phoneText = view.findViewById(R.id.phoneText);
        logoutButton = view.findViewById(R.id.logoutButton);
        bloodTypeContainer = view.findViewById(R.id.bloodTypeContainer);
        phoneContainer = view.findViewById(R.id.phoneContainer);
        supportButton = view.findViewById(R.id.supportButton);
        supportButton.setOnClickListener(v -> showSupportDialog());
    }

    private void showSupportDialog() {
        UserType userType = AuthManager.getInstance().getUserType();
        String[] supportOptions;

        switch (userType) {
            case DONOR:
                supportOptions = new String[]{
                        "How to use the app",
                        "How to create an event",
                        "Report an issue"
                };
                break;
            case SITE_MANAGER:
                supportOptions = new String[]{
                        "Help with creating events",
                        "How to use manager features",
                        "Request event cancellation",
                        "Report an issue"
                };
                break;
            case SUPER_USER:
                supportOptions = new String[]{
                        "System administration help",
                        "Report a technical issue"
                };
                break;
            default:
                supportOptions = new String[]{"Report an issue"};
                break;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("How can we help?")
                .setItems(supportOptions, (dialog, which) -> {
                    handleSupportOption(supportOptions[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSupportOption(String option) {
        switch (option) {
            case "How to create an event":
                showDonorEventCreationDialog();
                break;
            case "Help with creating events":
                notifySuperUserSupport("Event creation assistance needed");
                showManagerSupportConfirmation();
                break;
            case "Request event cancellation":
                notifySuperUserSupport("Event cancellation request");
                showManagerSupportConfirmation();
                break;
            case "How to use the app":
                showAppUsageGuide();
                break;
            case "How to use manager features":
                showManagerGuide();
                break;
            default:
                showReportIssueDialog();
                break;
        }
    }

    private void showDonorEventCreationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create Events")
                .setMessage("To create blood donation events, you need to register as an Event Organizer. Would you like to learn more about becoming an organizer?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Navigate to registration info or show more details
                    Toast.makeText(requireContext(),
                            "Please register a new account as an Event Organizer",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Not now", null)
                .show();
    }

    private void showManagerSupportConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Support Request Sent")
                .setMessage("An administrator will contact you soon to assist with your request.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void notifySuperUserSupport(String requestType) {
        String userId = AuthManager.getInstance().getUserId();
        String userName = AuthManager.getInstance().getUserName();

        NotificationManager notificationManager = new NotificationManager(
                requireContext(),
                ServiceLocator.getDatabaseHelper()
        );

        // Notify all super users
        try {
            List<User> superUsers = ServiceLocator.getUserRepository()
                    .findUsersByTimeRange(0, System.currentTimeMillis())
                    .stream()
                    .filter(user -> user.getUserType() == UserType.SUPER_USER)
                    .collect(Collectors.toList());

            for (User superUser : superUsers) {
                notificationManager.createEventNotification(
                        superUser.getUserId(),
                        null, // No event ID for support requests
                        "Support Request",
                        String.format("User %s needs assistance: %s", userName, requestType),
                        NotificationType.SYSTEM
                );
            }
        } catch (AppException e) {
            Log.e("ProfileFragment", "Error notifying super users", e);
        }
    }

    private void showAppUsageGuide() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Using the Blood Donor App")
                .setMessage(
                        "• Use the Map to find donation events near you\n" +
                                "• View event details and register to donate\n" +
                                "• Track your donation history\n" +
                                "• Receive notifications about upcoming events\n\n" +
                                "Need more help? Contact our support team.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showManagerGuide() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manager Features Guide")
                .setMessage(
                        "• Create and manage donation events\n" +
                                "• Track donor registrations\n" +
                                "• View event statistics\n" +
                                "• Generate reports\n" +
                                "• Manage volunteer assignments\n\n" +
                                "Need more help? Our support team is here to assist you.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showReportIssueDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Report an Issue")
                .setMessage("Please describe the issue you're experiencing. Our support team will review it and get back to you soon.")
                .setPositiveButton("Send", (dialog, which) -> {
                    // Here you could add actual issue reporting functionality
                    Toast.makeText(requireContext(),
                            "Thank you for your report. We'll review it shortly.",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupUserInfo() {
        try {
            // Get current user ID
            String userId = AuthManager.getInstance().getUserId();
            if (userId == null) return;

            // Get user details from repository
            ServiceLocator.getUserRepository()
                    .findById(userId)
                    .ifPresent(user -> {
                        // Set user information
                        nameText.setText(user.getFullName());
                        emailText.setText(user.getEmail());

                        UserType userType = user.getUserType();
                        if (userType == UserType.DONOR) {
                            userTypeText.setText("Blood Donor");
                            // Show blood type container, hide phone container
                            bloodTypeContainer.setVisibility(View.VISIBLE);
                            phoneContainer.setVisibility(View.GONE);
                            bloodTypeText.setText(user.getBloodType());
                        } else if (userType == UserType.SITE_MANAGER) {
                            userTypeText.setText("Event Manager");
                            // Show phone container, hide blood type container
                            bloodTypeContainer.setVisibility(View.GONE);
                            phoneContainer.setVisibility(View.VISIBLE);
                            phoneText.setText(user.getPhoneNumber());
                        }
                    });
        } catch (AppException e) {
            Toast.makeText(getContext(),
                    "Error loading profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            AuthManager.getInstance().logout(requireContext());
        });
    }
}