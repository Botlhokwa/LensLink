package com.example.lenslink.ui.dashboard;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.OfferingsActivity;
import com.example.lenslink.R;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.ui.photographer.PhotographerBookingsActivity;
import com.example.lenslink.util.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** Role-aware home screen. Booking navigation is offered to photographers only. */
public final class DashboardActivity extends AppCompatActivity {
    private AuthRepository auth;
    private LinearLayout content;
    private String role;
    private String fullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();
        if (!auth.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }
        UiUtils.enableEdgeToEdge(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(24), dp(22), dp(28));
        content.setBackgroundColor(getColor(R.color.lenslink_cream));
        scroll.addView(content);
        setContentView(scroll);
        UiUtils.applySystemBarInsets(scroll);
        content.addView(label("LensLink", 30, true, Typeface.SERIF));
        content.addView(label("Photography services, packages and extras", 14, false, null));
        content.addView(label("Loading your dashboard…", 15, false, null));

        auth.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                if (!Role.CLIENT.equals(user.getRole()) && !Role.PHOTOGRAPHER.equals(user.getRole())) {
                    auth.logout();
                    AppNavigator.goToLogin(DashboardActivity.this, getString(R.string.error_profile_missing));
                    return;
                }
                role = user.getRole();
                fullName = user.getFullName();
                renderDashboard();
            }
            @Override public void onProfileMissing() {
                if (!isFinishing() && !isDestroyed()) AppNavigator.goToCompleteProfile(DashboardActivity.this);
            }
            @Override public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                content.removeAllViews();
                content.addView(label("Could not load your profile", 22, true, null));
                content.addView(label(message, 14, false, null));
                content.addView(action("Retry", v -> recreate()));
                content.addView(action("Log out", v -> logout()));
            }
        });
    }

    private void renderDashboard() {
        content.removeAllViews();
        TextView logo = label("LensLink", 30, true, Typeface.SERIF);
        content.addView(logo);
        TextView welcome = label("Welcome, " + safe(fullName), 22, true, null);
        welcome.setPadding(0, dp(8), 0, dp(2));
        content.addView(welcome);
        content.addView(label(Role.CLIENT.equals(role) ? "READY TO CAPTURE YOUR NEXT MOMENT?" : "YOUR PHOTOGRAPHY STUDIO", 12, true, null));
        content.addView(label("QUICK ACTIONS", 13, true, null));

        if (Role.CLIENT.equals(role)) {
            addActionCard("Browse services and packages", "Explore Barred Productions offerings", "Browse offerings", v -> openOfferings(false));
            addActionCard("Chat assistant", "Get help choosing a package", "Open chat", v -> unavailable("Chat is not connected in this project yet."));
            content.addView(label("Your bookings are not shown in the client app.", 13, false, null));
        } else {
            addActionCard("Manage my offerings", "Update services, packages and add-ons", "Manage offerings", v -> openOfferings(true));
            addActionCard("Photographer bookings", "View bookings for your packages", "View bookings", v -> startActivity(new Intent(this, PhotographerBookingsActivity.class)));
            addActionCard("Browse offerings", "See the client catalogue", "Browse", v -> openOfferings(false));
        }

        content.addView(label("ACCOUNT", 13, true, null));
        addAction("My profile", v -> AppNavigator.goToProfile(this));
        addAction("Log out", v -> logout());
    }

    private void addActionCard(String title, String detail, String buttonText, View.OnClickListener listener) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(getColor(R.color.lenslink_card));
        LinearLayout inside = new LinearLayout(this);
        inside.setOrientation(LinearLayout.VERTICAL);
        inside.setPadding(dp(18), dp(14), dp(18), dp(14));
        inside.addView(label(title, 18, true, null));
        inside.addView(label(detail, 14, false, null));
        MaterialButton button = action(buttonText, listener);
        inside.addView(button);
        card.addView(inside);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10);
        p.bottomMargin = dp(8);
        content.addView(card, p);
    }

    private void addAction(String text, View.OnClickListener listener) {
        MaterialButton button = action(text, listener);
        content.addView(button);
    }

    private MaterialButton action(String text, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50));
        p.topMargin = dp(6);
        button.setLayoutParams(p);
        return button;
    }

    private TextView label(String value, int size, boolean bold, Typeface typeface) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(getColor(R.color.lenslink_text));
        if (bold) text.setTypeface(typeface == null ? Typeface.DEFAULT : typeface, Typeface.BOLD);
        else if (typeface != null) text.setTypeface(typeface);
        text.setPadding(0, dp(5), 0, dp(5));
        return text;
    }

    private void openOfferings(boolean manage) {
        Intent intent = new Intent(this, OfferingsActivity.class);
        intent.putExtra(OfferingsActivity.EXTRA_MODE,
                manage ? OfferingsActivity.MODE_MANAGE : OfferingsActivity.MODE_BROWSE);
        startActivity(intent);
    }

    private void logout() {
        auth.logout();
        AppNavigator.goToLogin(this);
    }

    private void unavailable(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String safe(String value) { return value == null || value.trim().isEmpty() ? "there" : value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
