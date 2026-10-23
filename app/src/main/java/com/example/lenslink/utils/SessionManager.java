package com.example.lenslink.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.lenslink.models.User;

/** Remembers who is signed in and which role they hold. Role is the single source for RBAC checks. */
public final class SessionManager {
    private static final String PREFS = "lenslink_session";
    private static final String K_ID = "userId";
    private static final String K_NAME = "name";
    private static final String K_EMAIL = "email";
    private static final String K_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(User user) {
        prefs.edit()
            .putString(K_ID, user.getUserId())
            .putString(K_NAME, user.getFullName())
            .putString(K_EMAIL, user.getEmail())
            .putString(K_ROLE, user.getRole() == null ? FirestoreConstants.ROLE_CLIENT : user.getRole().toLowerCase())
            .apply();
    }

    public boolean isLoggedIn() { return prefs.getString(K_ROLE, null) != null; }
    public String getUserId() { return prefs.getString(K_ID, ""); }
    public String getEmail() { return prefs.getString(K_EMAIL, ""); }
    public String getRole() { return prefs.getString(K_ROLE, ""); }

    public String getName() {
        String name = prefs.getString(K_NAME, "");
        if (name != null && !name.trim().isEmpty()) return name;
        String email = getEmail();
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "there";
    }

    public boolean isPhotographer() { return FirestoreConstants.ROLE_PHOTOGRAPHER.equalsIgnoreCase(getRole()); }
    public boolean isClient() { return FirestoreConstants.ROLE_CLIENT.equalsIgnoreCase(getRole()); }

    public void clear() { prefs.edit().clear().apply(); }
}
