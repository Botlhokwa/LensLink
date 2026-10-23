package com.example.lenslink.utils;

/** Feature-level role based access control. UI and repositories both ask this class. */
public final class Permissions {
    private Permissions() {}

    public enum Feature {
        BROWSE_SERVICES, BOOK_SESSION, VIEW_OWN_BOOKINGS, CANCEL_OWN_BOOKING, DOWNLOAD_PHOTOS,
        AI_CHAT, DIRECT_MESSAGE,
        MANAGE_BOOKINGS, UPLOAD_PHOTOS, MANAGE_PACKAGES
    }

    public static boolean can(String role, Feature feature) {
        if (role == null) return false;
        if (FirestoreConstants.ROLE_CLIENT.equalsIgnoreCase(role)) {
            switch (feature) {
                case BROWSE_SERVICES:
                case BOOK_SESSION:
                case VIEW_OWN_BOOKINGS:
                case CANCEL_OWN_BOOKING:
                case DOWNLOAD_PHOTOS:
                case AI_CHAT:
                case DIRECT_MESSAGE:
                    return true;
                default:
                    return false;
            }
        }
        if (FirestoreConstants.ROLE_PHOTOGRAPHER.equalsIgnoreCase(role)) {
            switch (feature) {
                case DIRECT_MESSAGE:
                case MANAGE_BOOKINGS:
                case UPLOAD_PHOTOS:
                case MANAGE_PACKAGES:
                    return true;
                default:
                    return false; // photographers never get the AI assistant
            }
        }
        return false;
    }

    public static String label(String role) {
        return FirestoreConstants.ROLE_PHOTOGRAPHER.equalsIgnoreCase(role) ? "Photographer / Studio" : "Client";
    }
}
