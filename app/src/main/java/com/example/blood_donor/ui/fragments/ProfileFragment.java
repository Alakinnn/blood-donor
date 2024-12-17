package com.example.blood_donor.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.blood_donor.R;
import com.example.blood_donor.server.models.user.UserType;
import com.example.blood_donor.ui.manager.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class ProfileFragment extends Fragment {
    private MaterialTextView nameText;
    private MaterialTextView emailText;
    private MaterialTextView userTypeText;
    private MaterialTextView bloodTypeText;
    private MaterialTextView phoneText;
    private MaterialButton logoutButton;

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
    }

    private void setupUserInfo() {
        AuthManager authManager = AuthManager.getInstance();
        nameText.setText(authManager.getUserName());

        // Show/hide fields based on user type
        UserType userType = authManager.getUserType();
        if (userType == UserType.DONOR) {
            userTypeText.setText("Donor");
            bloodTypeText.setVisibility(View.VISIBLE);
            phoneText.setVisibility(View.GONE);
        } else if (userType == UserType.SITE_MANAGER) {
            userTypeText.setText("Event Manager");
            bloodTypeText.setVisibility(View.GONE);
            phoneText.setVisibility(View.VISIBLE);
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            AuthManager.getInstance().logout(requireContext());
        });
    }
}