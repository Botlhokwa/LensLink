package com.example.lenslink.navigation;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.example.lenslink.model.Role;
import com.example.lenslink.ui.auth.CompleteProfileActivity;
import com.example.lenslink.ui.auth.LoginActivity;
import com.example.lenslink.ui.profile.ProfileActivity;
import com.example.lenslink.ui.dashboard.DashboardActivity;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static boolean goToHomeForRole(Context context, String role) {
        if (Role.CLIENT.equals(role)) {
            goToClientHome(context);
            return true;
        }
        if (Role.PHOTOGRAPHER.equals(role)) {
            goToPhotographerHome(context);
            return true;
        }
        return false;
    }

    public static void goToClientHome(Context context) {
        startAsNewRoot(context, DashboardActivity.class);
    }

    public static void goToPhotographerHome(Context context) {
        startAsNewRoot(context, DashboardActivity.class);
    }

    public static void goToLogin(Context context) {
        goToLogin(context, null);
    }

    public static void goToLogin(Context context, @Nullable String message) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (message != null) {
            intent.putExtra(LoginActivity.EXTRA_MESSAGE, message);
        }
        context.startActivity(intent);
    }

    public static void goToCompleteProfile(Context context) {
        startAsNewRoot(context, CompleteProfileActivity.class);
    }

    public static void goToProfile(Context context) {
        context.startActivity(new Intent(context, ProfileActivity.class));
    }

    private static void startAsNewRoot(Context context, Class<?> target) {
        Intent intent = new Intent(context, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
