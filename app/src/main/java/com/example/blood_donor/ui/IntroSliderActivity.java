package com.example.blood_donor.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.blood_donor.R;
import com.example.blood_donor.ui.manager.AuthManager;
import com.google.android.material.button.MaterialButton;

public class IntroSliderActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnNext;
    private MaterialButton btnSkip;
    private IntroSliderAdapter sliderAdapter;
    private SharedPreferences prefs;

    private static final String PREF_NAME = "IntroSlider";
    private static final String IS_FIRST_TIME = "IsFirstTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_slider);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (!isFirstTime()) {
            navigateToNextScreen();
            finish();
            return;
        }

        initViews();
        setupViewPager();
        setupButtons();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
    }

    private void setupViewPager() {
        sliderAdapter = new IntroSliderAdapter();
        viewPager.setAdapter(sliderAdapter);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        setupDots(0);
    }

    private void setupButtons() {
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < sliderAdapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                markAsShown();
                navigateToNextScreen();
            }
        });

        btnSkip.setOnClickListener(v -> {
            markAsShown();
            navigateToNextScreen();
        });
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    setupDots(position);

                    if (position == sliderAdapter.getItemCount() - 1) {
                        btnNext.setText(R.string.start);
                        btnSkip.setVisibility(View.GONE);
                    } else {
                        btnNext.setText(R.string.next);
                        btnSkip.setVisibility(View.VISIBLE);
                    }
                }
            };

    private void setupDots(int currentPage) {
        ImageView[] dots = new ImageView[sliderAdapter.getItemCount()];
        layoutDots.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            int drawable = (i == currentPage) ?
                    R.drawable.dot_active : R.drawable.dot_inactive;
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, drawable));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            layoutDots.addView(dots[i], params);
        }
    }

    private boolean isFirstTime() {
        return prefs.getBoolean(IS_FIRST_TIME, true);
    }

    private void markAsShown() {
        prefs.edit().putBoolean(IS_FIRST_TIME, false).apply();
    }

    private void navigateToNextScreen() {
        // Check if user is logged in
        boolean isLoggedIn = AuthManager.getInstance().isLoggedIn();
        Intent intent = new Intent(this,
                isLoggedIn ? MainActivity.class : LoginActivity.class);
        startActivity(intent);
        finish();
    }
}