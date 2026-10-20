package com.example.lenslink.database;

public class user {
    private String uid;
    private String fullName;
    private String phone;
    private String email;
    private boolean isAdmin; // true for you, false for clients

    public user() {} // Required by Firebase

    public user(String uid, String fullName, String phone, String email, boolean isAdmin) {
        this.uid = uid;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.isAdmin = isAdmin;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
