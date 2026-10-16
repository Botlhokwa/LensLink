package com.example.lenslink.model;
public class User {
    private String userId;    // same value as the Firebase Auth UID and the document ID
    private String fullName;
    private String email;
    private String phone;     // optional, may be null
    private String role;      // Role.CLIENT or Role.PHOTOGRAPHER
    private String studioId;  // empty at registration, assigned later by a controlled process

    public User() {
    }

    public User(String userId, String fullName, String email,
                String phone, String role, String studioId) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.studioId = studioId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudioId() { return studioId; }
    public void setStudioId(String studioId) { this.studioId = studioId; }
}