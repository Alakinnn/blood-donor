package com.example.blood_donor.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.auth.ManagerRegisterRequest;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.interfaces.IUserService;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ManagerRegistrationActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText dobInput;
    private AutoCompleteTextView genderInput;
    private MaterialButton registerButton;
    private MaterialTextView loginLink;
    private final Calendar calendar = Calendar.getInstance();
    private final IUserService userService;

    private static final String[] GENDERS = new String[]{"Male", "Female", "Other"};

    public ManagerRegistrationActivity() {
        this.userService = ServiceLocator.getUserService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_registration);

        initializeViews();
        setupDropdowns();
        setupDatePicker();
        setupClickListeners();
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        dobInput = findViewById(R.id.dobInput);
        genderInput = findViewById(R.id.genderInput);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
    }

    private void setupDropdowns() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, GENDERS);
        genderInput.setAdapter(genderAdapter);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateLabel();
        };

        dobInput.setOnClickListener(v -> new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateLabel() {
        String dateFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        dobInput.setText(sdf.format(calendar.getTime()));
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> handleRegistration());
        loginLink.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        ManagerRegisterRequest request = new ManagerRegisterRequest();
        request.setFullName(nameInput.getText().toString().trim());
        request.setEmail(emailInput.getText().toString().trim());
        request.setPhoneNumber(phoneInput.getText().toString().trim());
        request.setPassword(passwordInput.getText().toString().trim());
        request.setDateOfBirth(calendar.getTimeInMillis());
        request.setGender(genderInput.getText().toString());

        try {
            request.validate();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        ApiResponse<?> response = userService.registerManager(request);
        if (response.isSuccess()) {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
            AuthManager.getInstance().navigateToAppropriateScreen(this);
            finish();
        } else {
            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }
}