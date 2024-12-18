package com.example.blood_donor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.auth.LoginRequest;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.interfaces.IUserService;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialTextView donorSignUpText;
    private MaterialTextView managerSignUpText;
    private final IUserService userService;

    public LoginActivity() {
        this.userService = ServiceLocator.getUserService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        donorSignUpText = findViewById(R.id.donorSignUpText);
        managerSignUpText = findViewById(R.id.managerSignUpText);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleLogin());

        donorSignUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, DonorRegistrationActivity.class);
            startActivity(intent);
        });

        managerSignUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ManagerRegistrationActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ApiResponse<?> response = userService.login(loginRequest);

        if (response.isSuccess()) {
            Intent intent = new Intent(LoginActivity.this, BaseActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}