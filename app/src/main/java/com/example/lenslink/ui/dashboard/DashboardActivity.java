package com.example.lenslink.ui.dashboard;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.OfferingsActivity;
import com.example.lenslink.R;
import com.example.lenslink.ui.client.BookSessionActivity;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.ui.photographer.PhotographerBookingsActivity;
import com.example.lenslink.util.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/** Role-aware home screen. Booking navigation is offered to photographers only. */
public final class DashboardActivity extends AppCompatActivity {
    private AuthRepository auth;
    private LinearLayout content;
    private String role;
    private String fullName;
    private String userId;
    private LinearLayout bookingUpdates;
    private ListenerRegistration bookingUpdatesListener;
    private final Map<String, String> seenBookingStatuses = new HashMap<>();

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
                userId = user.getUserId();
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
            addActionCard("Book a session", "Choose a package, services, date and time", "Book a session", v -> startActivity(new Intent(this, BookSessionActivity.class)));
            addActionCard("Browse services and packages", "Explore Barred Productions offerings", "Browse offerings", v -> openOfferings(false));
            addActionCard("Chat assistant", "Get help choosing a package", "Open chat", v -> unavailable("Chat is not connected in this project yet."));
            content.addView(label("BOOKING UPDATES", 13, true, null));
            bookingUpdates = new LinearLayout(this);
            bookingUpdates.setOrientation(LinearLayout.VERTICAL);
            content.addView(bookingUpdates);
            listenForBookingUpdates();
        } else {
            addActionCard("Manage my offerings", "Update services, packages and add-ons", "Manage offerings", v -> openOfferings(true));
            addActionCard("Photographer bookings", "View bookings for your packages", "View bookings", v -> startActivity(new Intent(this, PhotographerBookingsActivity.class)));
            addActionCard("Browse offerings", "See the client catalogue", "Browse", v -> openOfferings(false));
        }

        content.addView(label("ACCOUNT", 13, true, null));
        addAction("My profile", v -> AppNavigator.goToProfile(this));
        addAction("Log out", v -> logout());
    }

    private void listenForBookingUpdates() {
        if (!Role.CLIENT.equals(role) || userId == null || bookingUpdates == null
                || bookingUpdatesListener != null) return;
        bookingUpdatesListener = FirebaseFirestore.getInstance().collection("Booking")
                .whereEqualTo("clientId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed() || bookingUpdates == null) return;
                    if (error != null) {
                        bookingUpdates.removeAllViews();
                        bookingUpdates.addView(label("Could not load booking updates.", 14, false, null));
                        return;
                    }
                    if (snapshot == null) return;
                    bookingUpdates.removeAllViews();
                    int updateCount = 0;
                    for (DocumentSnapshot booking : snapshot.getDocuments()) {
                        String currentStatus = booking.getString("status");
                        String previousStatus = seenBookingStatuses.put(booking.getId(), currentStatus);
                        if (previousStatus != null && currentStatus != null
                                && !previousStatus.equalsIgnoreCase(currentStatus)
                                && ("confirmed".equalsIgnoreCase(currentStatus)
                                || "rejected".equalsIgnoreCase(currentStatus))) {
                            String message = "Your booking request was "
                                    + ("confirmed".equalsIgnoreCase(currentStatus) ? "accepted." : "rejected.");
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        }
                        if (currentStatus == null || (!"confirmed".equalsIgnoreCase(currentStatus)
                                && !"rejected".equalsIgnoreCase(currentStatus))) continue;
                        updateCount++;
                        String packageName = booking.getString("packageName");
                        String decision = "confirmed".equalsIgnoreCase(currentStatus) ? "accepted" : "rejected";
                        String message = packageName == null || packageName.trim().isEmpty()
                                ? "Your booking request was " + decision + "."
                                : "Your " + packageName + " request was " + decision + ".";
                        String date = readableBookingDate(booking.get("bookedDates"));
                        if (!date.isEmpty()) message += " Date: " + date + ".";
                        MaterialCardView card = new MaterialCardView(this);
                        card.setRadius(dp(14));
                        card.setCardElevation(dp(1));
                        card.setCardBackgroundColor(getColor(R.color.lenslink_card));
                        TextView update = label(message, 14, false, null);
                        update.setPadding(dp(14), dp(10), dp(14), dp(10));
                        card.addView(update);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                        params.topMargin = dp(6);
                        bookingUpdates.addView(card, params);
                    }
                    if (updateCount == 0) {
                        bookingUpdates.addView(label("Accepted or rejected booking requests will appear here.", 14, false, null));
                    }
                });
    }

    private String readableBookingDate(Object value) {
        if (!(value instanceof java.util.List<?>)) return "";
        StringBuilder result = new StringBuilder();
        for (Object date : (java.util.List<?>) value) {
            if (date == null) continue;
            if (result.length() > 0) result.append(", ");
            result.append(date);
        }
        return result.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForBookingUpdates();
    }

    @Override
    protected void onStop() {
        if (bookingUpdatesListener != null) {
            bookingUpdatesListener.remove();
            bookingUpdatesListener = null;
        }
        super.onStop();
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
