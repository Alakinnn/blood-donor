package com.example.blood_donor.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.errors.AppException;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
public class ProfileFragment extends Fragment {
    private MaterialTextView nameText;
    private MaterialTextView emailText;
    private MaterialTextView userTypeText;
    private MaterialTextView bloodTypeText;
    private MaterialTextView phoneText;
    private MaterialButton logoutButton;
    private View bloodTypeContainer;
    private View phoneContainer;

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