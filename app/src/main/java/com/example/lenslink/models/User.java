package com.example.lenslink.models;
public class User {
    private String userId, fullName, email, phone, role, profileImageUrl;
    private long createdAt;
    public User() {}
    public User(String userId, String fullName, String email, String phone, String role) {
        this.userId = userId; this.fullName = fullName; this.email = email; this.phone = phone; this.role = role;
        this.createdAt = System.currentTimeMillis();
    }
    public String getUserId() { return userId; } public void setUserId(String v) { userId = v; }
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getRole() { return role; } public void setRole(String v) { role = v; }
    public String getProfileImageUrl() { return profileImageUrl; } public void setProfileImageUrl(String v) { profileImageUrl = v; }
    public long getCreatedAt() { return createdAt; } public void setCreatedAt(long v) { createdAt = v; }
}
