package com.example.blood_donor.dto.events;

public class BloodTypeProgress {
    private String bloodType;
    private double targetAmount;
    private double collectedAmount;
    private double progressPercentage;

    public BloodTypeProgress(double targetAmount, double collectedAmount) {
        this.targetAmount = targetAmount;
        this.collectedAmount = collectedAmount;
    }
}
