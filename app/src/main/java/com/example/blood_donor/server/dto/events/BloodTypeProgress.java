package com.example.blood_donor.server.dto.events;

public class BloodTypeProgress {
    private String bloodType;
    private double targetAmount;
    private double collectedAmount;
    private double progressPercentage;

    public BloodTypeProgress(String bloodType, double targetAmount, double collectedAmount) {
        this.targetAmount = targetAmount;
        this.collectedAmount = collectedAmount;
        this.bloodType = bloodType;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCollectedAmount() {
        return collectedAmount;
    }

    public void setCollectedAmount(double collectedAmount) {
        this.collectedAmount = collectedAmount;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
