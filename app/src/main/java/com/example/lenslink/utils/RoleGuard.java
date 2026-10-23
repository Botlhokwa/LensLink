package com.example.lenslink.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.example.lenslink.LoginActivity;
import com.example.lenslink.MainActivity;
import com.example.lenslink.PhotographerActivity;
import com.google.firebase.auth.FirebaseAuth;

/** Call first thing in every role-specific Activity. Redirects anyone who should not be there. */
public final class RoleGuard {
    private RoleGuard() {}

    public static boolean enforce(Activity activity, SessionManager session, String requiredRole) {
        boolean signedIn = session.isLoggedIn() && FirebaseAuth.getInstance().getCurrentUser() != null;
        if (!signedIn) {
            session.clear();
            go(activity, LoginActivity.class);
            return false;
        }
        if (!requiredRole.equalsIgnoreCase(session.getRole())) {
            Toast.makeText(activity, "That area isn't available for your account type.", Toast.LENGTH_SHORT).show();
            go(activity, session.isPhotographer() ? PhotographerActivity.class : MainActivity.class);
            return false;
        }
        return true;
    }

    public static void go(Activity from, Class<?> target) {
        Intent i = new Intent(from, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(i);
        from.finish();
    }

    public static void routeByRole(Activity from, String role) {
        go(from, FirestoreConstants.ROLE_PHOTOGRAPHER.equalsIgnoreCase(role) ? PhotographerActivity.class : MainActivity.class);
    }
}
