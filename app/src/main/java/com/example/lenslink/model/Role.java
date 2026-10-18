package com.example.lenslink.model;
public final class Role {
    public static final String CLIENT = "CLIENT";
    public static final String PHOTOGRAPHER = "PHOTOGRAPHER";
    private Role() {
        // Constants holder, not meant to be instantiated.
    }

    public static boolean isSelfRegistrationRole(String role) {
        return CLIENT.equals(role) || PHOTOGRAPHER.equals(role);
    }
}