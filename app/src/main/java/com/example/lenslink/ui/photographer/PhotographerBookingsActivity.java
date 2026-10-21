package com.example.lenslink.ui.photographer;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.util.UiUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Photographer-only booking browser. It scopes records to this user's owned package IDs. */
public final class PhotographerBookingsActivity extends AppCompatActivity {
    private AuthRepository auth;
    private LinearLayout list;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();
        if (!auth.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }
        UiUtils.enableEdgeToEdge(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(getColor(R.color.lenslink_cream));
        scroll.addView(root);
        setContentView(scroll);
        UiUtils.applySystemBarInsets(scroll);
        root.addView(text("Photographer bookings", 25, true));
        root.addView(text("Only bookings tied to your packages are listed here.", 14, false));
        MaterialButton back = new MaterialButton(this);
        back.setText("Back to dashboard");
        back.setAllCaps(false);
        back.setOnClickListener(v -> AppNavigator.goToPhotographerHome(this));
        root.addView(back);
        status = text("Checking photographer access…", 14, false);
        root.addView(status);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        auth.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                if (!Role.PHOTOGRAPHER.equals(user.getRole())) {
                    AppNavigator.goToClientHome(PhotographerBookingsActivity.this);
                    return;
                }
                loadBookings(user.getUserId());
            }
            @Override public void onProfileMissing() {
                if (!isFinishing() && !isDestroyed()) AppNavigator.goToCompleteProfile(PhotographerBookingsActivity.this);
            }
            @Override public void onError(String message) { showError(message); }
        });
    }

    private void loadBookings(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        status.setText("Loading your bookings…");
        db.collection("packages").whereEqualTo("photographerId", uid).get()
                .addOnSuccessListener(packages -> {
                    if (isFinishing() || isDestroyed()) return;
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot document : packages.getDocuments()) ids.add(document.getId());
                    List<Task<QuerySnapshot>> queries = new ArrayList<>();
                    // Supports new booking documents that store the photographer UID directly.
                    queries.add(db.collection("Booking").whereEqualTo("photographerId", uid).get());
                    // Existing booking schema links to packageId, so resolve the photographer's
                    // package IDs first and fetch only matching bookings.
                    for (int start = 0; start < ids.size(); start += 30) {
                        List<String> chunk = new ArrayList<>(ids.subList(start, Math.min(start + 30, ids.size())));
                        if (!chunk.isEmpty()) queries.add(db.collection("Booking").whereIn("packageId", chunk).get());
                    }
                    Tasks.whenAllSuccess(queries).addOnSuccessListener(results -> {
                        if (isFinishing() || isDestroyed()) return;
                        Map<String, DocumentSnapshot> unique = new LinkedHashMap<>();
                        for (Object result : results) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (DocumentSnapshot document : snapshot.getDocuments()) unique.put(document.getId(), document);
                        }
                        render(new ArrayList<>(unique.values()));
                    }).addOnFailureListener(this::showFailure);
                }).addOnFailureListener(this::showFailure);
    }

    private void render(List<DocumentSnapshot> bookings) {
        list.removeAllViews();
        if (bookings.isEmpty()) {
            status.setText("No bookings found for your packages yet.");
            return;
        }
        status.setText(bookings.size() + (bookings.size() == 1 ? " booking" : " bookings"));
        for (DocumentSnapshot document : bookings) {
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(getColor(R.color.lenslink_card));
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(16), dp(12), dp(16), dp(12));
            card.addView(body);
            body.addView(text("Booking " + document.getId(), 17, true));
            addField(body, "Status", document.getString("status"));
            addField(body, "Package", document.getString("packageId"));
            addField(body, "Client ID", document.getString("clientId"));
            addField(body, "Dates", stringList(document.get("bookedDates")));
            addField(body, "Time", document.getString("startTime"));
            addField(body, "Location", document.getString("location"));
            Object price = document.get("totalPrice");
            if (price != null) addField(body, "Total", "R " + price);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(12);
            list.addView(card, params);
        }
    }

    private void addField(LinearLayout parent, String label, String value) {
        if (value != null && !value.trim().isEmpty()) parent.addView(text(label + ": " + value, 14, false));
    }

    private String stringList(Object value) {
        if (!(value instanceof List<?>)) return value == null ? "" : String.valueOf(value);
        List<?> items = (List<?>) value;
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            if (result.length() > 0) result.append(", ");
            result.append(item);
        }
        return result.toString();
    }

    private void showFailure(Exception error) {
        if (isFinishing() || isDestroyed()) return;
        showError(error.getLocalizedMessage() == null ? "Could not load bookings." : error.getLocalizedMessage());
    }

    private void showError(String message) {
        if (isFinishing() || isDestroyed()) return;
        status.setText("Could not load bookings: " + message);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.lenslink_text));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(5), 0, dp(5));
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
