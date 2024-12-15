package com.example.blood_donor.models.event;

public class BloodTypeRequirement {
    private String bloodType;
    private double targetAmount;    // in liters
    private double collectedAmount; // in liters

    public BloodTypeRequirement(String bloodType, double targetAmount) {
        this.bloodType = bloodType;
        this.targetAmount = targetAmount;
        this.collectedAmount = 0.0;
    }

    public void addDonation(double amount) {
        this.collectedAmount += amount;
    }

    public double getRemainingAmount() {
        return Math.max(0, targetAmount - collectedAmount);
    }

    public double getProgress() {
        return targetAmount > 0 ? (collectedAmount / targetAmount) * 100 : 0;
    }

    // Getters and setters
    public String getBloodType() { return bloodType; }
    public double getTargetAmount() { return targetAmount; }
    public double getCollectedAmount() { return collectedAmount; }
}
