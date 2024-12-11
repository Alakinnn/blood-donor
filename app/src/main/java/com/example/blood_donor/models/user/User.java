package com.example.blood_donor.models.user;

public class User {
    private String userId;
    private String email;
    private String password;
    private String fullName;
    private long dateOfBirth;
    private String phoneNumber;
    private UserType userType;
    private String bloodType;
    private String gender;

    // Constructor
    public User(String userId, String email, String password, String fullName,
                long dateOfBirth, String phoneNumber, UserType userType,
                String bloodType, String gender) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.bloodType = bloodType;
        this.gender = gender;
    }

    // Getters and setters
    // ... (all standard getters and setters)
}
