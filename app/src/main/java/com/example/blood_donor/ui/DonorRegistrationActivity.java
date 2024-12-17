package com.example.blood_donor.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood_donor.R;
import com.example.blood_donor.server.dto.auth.AuthResponse;
import com.example.blood_donor.server.dto.auth.DonorRegisterRequest;
import com.example.blood_donor.server.models.response.ApiResponse;
import com.example.blood_donor.server.services.interfaces.IUserService;
import com.example.blood_donor.ui.manager.AuthManager;
import com.example.blood_donor.ui.manager.ServiceLocator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DonorRegistrationActivity extends AppCompatActivity {
    private static final String TAG = "DonorRegistration";
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText dobInput;
    private AutoCompleteTextView bloodTypeInput;
    private AutoCompleteTextView genderInput;
    private MaterialButton registerButton;
    private MaterialTextView loginLink;
    private CircularProgressIndicator progressIndicator;
    private final Calendar calendar = Calendar.getInstance();
    private final IUserService userService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] BLOOD_TYPES = new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private static final String[] GENDERS = new String[]{"Male", "Female", "Other"};

    public DonorRegistrationActivity() {
        this.userService = ServiceLocator.getUserService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_registration);

        initializeViews();
        setupDropdowns();
        setupDatePicker();
        setupClickListeners();
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        dobInput = findViewById(R.id.dobInput);
        bloodTypeInput = findViewById(R.id.bloodTypeInput);
        genderInput = findViewById(R.id.genderInput);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupDropdowns() {
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, BLOOD_TYPES);
        bloodTypeInput.setAdapter(bloodTypeAdapter);

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
        try {
            // Disable inputs and show progress
            setLoading(true);

            DonorRegisterRequest request = new DonorRegisterRequest();
            request.setFullName(nameInput.getText().toString().trim());
            request.setEmail(emailInput.getText().toString().trim());
            request.setPassword(passwordInput.getText().toString().trim());
            request.setDateOfBirth(calendar.getTimeInMillis());
            request.setBloodType(bloodTypeInput.getText().toString());
            request.setGender(genderInput.getText().toString());

            // Validate before starting background task
            request.validate();

            executorService.execute(() -> {
                SQLiteDatabase db = null;
                try {
                    ApiResponse<?> response = userService.registerDonor(request);

                    mainHandler.post(() -> {
                        try {
                            if (response.isSuccess()) {
                                AuthResponse authResponse = (AuthResponse) response.getData();
                                AuthManager.getInstance().saveAuthToken(
                                        authResponse.getToken(),
                                        authResponse.getUser().getUserId(),
                                        authResponse.getUser().getUserType(),
                                        authResponse.getUser().getFullName()
                                );
                                AuthManager.getInstance().navigateToAppropriateScreen(this);
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling registration response", e);
                            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_SHORT).show();
                        } finally {
                            setLoading(false);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during registration", e);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
                } finally {
                    if (db != null) {
                        try {
                            db.close();
                        } catch (Exception e) {
                            Log.e(TAG, "Error closing database", e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Validation error", e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressIndicator.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            nameInput.setEnabled(false);
            emailInput.setEnabled(false);
            passwordInput.setEnabled(false);
            dobInput.setEnabled(false);
            bloodTypeInput.setEnabled(false);
            genderInput.setEnabled(false);
        } else {
            progressIndicator.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            nameInput.setEnabled(true);
            emailInput.setEnabled(true);
            passwordInput.setEnabled(true);
            dobInput.setEnabled(true);
            bloodTypeInput.setEnabled(true);
            genderInput.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}