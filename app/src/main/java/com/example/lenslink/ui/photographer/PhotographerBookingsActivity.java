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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Photographer-only booking browser. It scopes records to this user's owned package IDs. */
public final class PhotographerBookingsActivity extends AppCompatActivity {
    private AuthRepository auth;
    private LinearLayout list;
    private TextView status;
    private String photographerUid;
    private final Set<String> ownedPackageIds = new HashSet<>();
    private final Map<String, String> ownedPackageNames = new LinkedHashMap<>();

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
                photographerUid = user.getUserId();
                loadBookings(photographerUid);
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
                    ownedPackageIds.clear();
                    ownedPackageIds.addAll(ids);
                    ownedPackageNames.clear();
                    for (DocumentSnapshot packageDocument : packages.getDocuments()) {
                        String name = packageDocument.getString("name");
                        String category = packageDocument.getString("category");
                        if (name != null && category != null && !category.trim().isEmpty()) {
                            name = category + " · " + name;
                        }
                        if (name != null) ownedPackageNames.put(packageDocument.getId(), name);
                    }
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
            body.addView(text("Booking request", 17, true));
            addField(body, "Status", document.getString("status"));
            String packageId = document.getString("packageId");
            String packageName = document.getString("packageName");
            if (packageName == null && packageId != null) packageName = ownedPackageNames.get(packageId);
            if (packageName == null) packageName = packageId == null ? "No package" : "Package name unavailable";
            addField(body, "Package", packageName);
            addField(body, "Services", displayNames(document.get("serviceNames"), document.get("serviceIds"), "No additional services"));
            addField(body, "Add-ons", displayNames(document.get("addonNames"), document.get("addonIds"), "No add-ons"));
            String clientName = document.getString("clientName");
            addField(body, "Client", clientName == null || clientName.trim().isEmpty()
                    ? "Name unavailable for this earlier booking" : clientName);
            addField(body, "Dates", stringList(document.get("bookedDates")));
            addField(body, "Time", document.getString("startTime"));
            addField(body, "Until", document.getString("bufferEndTime"));
            addField(body, "Location", document.getString("location"));
            Object price = document.get("totalPrice");
            if (price != null) addField(body, "Total", "R " + price);
            String bookingStatus = document.getString("status");
            if (bookingStatus != null && "pending".equalsIgnoreCase(bookingStatus.trim())) {
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                MaterialButton accept = new MaterialButton(this);
                accept.setText("Accept booking");
                accept.setAllCaps(false);
                MaterialButton reject = new MaterialButton(this);
                reject.setText("Reject booking");
                reject.setAllCaps(false);
                accept.setOnClickListener(v -> updateBookingStatus(document, "confirmed", accept, reject));
                reject.setOnClickListener(v -> updateBookingStatus(document, "rejected", accept, reject));
                LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(0, -2, 1f);
                actions.addView(accept, acceptParams);
                LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, -2, 1f);
                rejectParams.leftMargin = dp(8);
                actions.addView(reject, rejectParams);
                body.addView(actions);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(12);
            list.addView(card, params);
        }
    }

    private void updateBookingStatus(DocumentSnapshot booking, String newStatus,
                                     MaterialButton acceptButton, MaterialButton rejectButton) {
        if (photographerUid == null) return;
        String bookingPhotographerId = booking.getString("photographerId");
        String packageId = booking.getString("packageId");
        boolean belongsToPhotographer = photographerUid.equals(bookingPhotographerId)
                || (packageId != null && ownedPackageIds.contains(packageId));
        if (!belongsToPhotographer) {
            status.setText("You can only update bookings for your own packages.");
            return;
        }
        acceptButton.setEnabled(false);
        rejectButton.setEnabled(false);
        acceptButton.setText("Accept");
        rejectButton.setText("Reject");
        boolean accepting = "confirmed".equals(newStatus);
        (accepting ? acceptButton : rejectButton).setText(accepting ? "Accepting…" : "Rejecting…");
        FirebaseFirestore.getInstance().collection("Booking").document(booking.getId())
                .update("status", newStatus, "statusUpdatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;
                    status.setText("Booking " + (accepting ? "accepted." : "rejected."));
                    loadBookings(photographerUid);
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    acceptButton.setEnabled(true);
                    rejectButton.setEnabled(true);
                    acceptButton.setText("Accept booking");
                    rejectButton.setText("Reject booking");
                    status.setText("Could not update booking: "
                            + (error.getLocalizedMessage() == null ? "Please try again." : error.getLocalizedMessage()));
                });
    }

    private void addField(LinearLayout parent, String label, String value) {
        if (value != null && !value.trim().isEmpty()) parent.addView(text(label + ": " + value, 14, false));
    }

    private String displayNames(Object namesValue, Object idsValue, String emptyMessage) {
        String names = stringList(namesValue);
        if (!names.isEmpty()) return names;
        String ids = stringList(idsValue);
        return ids.isEmpty() ? emptyMessage : "Names unavailable for this earlier booking";
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
