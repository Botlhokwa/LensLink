package com.example.lenslink;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.lenslink.models.Booking;
import com.example.lenslink.models.Message;
import com.example.lenslink.models.Package;
import com.example.lenslink.models.Photo;
import com.example.lenslink.repositories.AuthRepository;
import com.example.lenslink.repositories.BookingRepository;
import com.example.lenslink.repositories.ChatRepository;
import com.example.lenslink.repositories.GalleryRepository;
import com.example.lenslink.repositories.PackageRepository;
import com.example.lenslink.repositories.RepositoryCallback;
import com.example.lenslink.ui.BottomNavBar;
import com.example.lenslink.utils.FirestoreConstants;
import com.example.lenslink.utils.Permissions;
import com.example.lenslink.utils.RoleGuard;
import com.example.lenslink.utils.SessionManager;
import com.example.lenslink.utils.SystemBars;
import com.example.lenslink.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Photographer / studio experience. Same visual language as the client app, but a different set of
 * features: manage bookings, deliver photos, publish packages and message clients. There is
 * deliberately no AI assistant here (see {@link Permissions}).
 */
public class PhotographerActivity extends AppCompatActivity {
    private static final String[] FILTERS = {"All", "Pending", "Confirmed", "Completed", "Cancelled"};

    private final BookingRepository bookingRepo = new BookingRepository();
    private final PackageRepository packageRepo = new PackageRepository();
    private final GalleryRepository galleryRepo = new GalleryRepository();
    private final ChatRepository chatRepo = new ChatRepository();
    private final AuthRepository authRepo = new AuthRepository();
    private SessionManager session;

    private View screenHome, screenBookings, screenPackages, screenMessages, screenProfile, currentScreen;
    private BottomNavBar nav;
    private LinearLayout pendingContainer, nextSessionContainer, bookingsContainer, filterChips,
        packagesContainer, chatContainer;
    private ScrollView chatScroll;
    private String activeFilter = "All";

    // Booking the photographer is currently delivering photos for, and the system gallery picker (multi-select).
    private Booking uploadTarget;
    private final ActivityResultLauncher<String> photoPicker =
        registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::onPhotosPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!RoleGuard.enforce(this, session, FirestoreConstants.ROLE_PHOTOGRAPHER)) return;
        setContentView(R.layout.activity_photographer);

        nav = findViewById(R.id.bottomNav);
        SystemBars.apply(this, findViewById(R.id.root), nav);

        screenHome = findViewById(R.id.pScreenHome);
        screenBookings = findViewById(R.id.pScreenBookings);
        screenPackages = findViewById(R.id.pScreenPackages);
        screenMessages = findViewById(R.id.pScreenMessages);
        screenProfile = findViewById(R.id.screenProfile);
        pendingContainer = findViewById(R.id.pendingContainer);
        nextSessionContainer = findViewById(R.id.nextSessionContainer);
        bookingsContainer = findViewById(R.id.pBookingsContainer);
        filterChips = findViewById(R.id.filterChips);
        packagesContainer = findViewById(R.id.pPackagesContainer);
        chatContainer = findViewById(R.id.pChatMessagesContainer);
        chatScroll = findViewById(R.id.pChatScroll);

        ((TextView) findViewById(R.id.tvGreetingName)).setText(session.getName());
        ((TextView) findViewById(R.id.tvGreetingAvatar)).setText(UiUtils.initial(session.getName()));

        // Role-shaped navigation: no AI chat tab for photographers.
        List<BottomNavBar.Item> items = new ArrayList<>();
        items.add(new BottomNavBar.Item(R.id.navHome, R.drawable.ic_home, "Home"));
        items.add(new BottomNavBar.Item(R.id.navBookings, R.drawable.ic_list, "Bookings"));
        items.add(new BottomNavBar.Item(R.id.navPackages, R.drawable.ic_grid, "Packages"));
        items.add(new BottomNavBar.Item(R.id.navMessages, R.drawable.ic_chat, "Messages"));
        items.add(new BottomNavBar.Item(R.id.navProfile, R.drawable.ic_person, "Profile"));
        nav.setItems(items);
        nav.setOnItemSelectedListener(id -> {
            if (id == R.id.navHome) openHome();
            else if (id == R.id.navBookings) openBookings("All");
            else if (id == R.id.navPackages) openPackages();
            else if (id == R.id.navMessages) openMessages();
            else if (id == R.id.navProfile) openProfile();
        });

        findViewById(R.id.cardPBookings).setOnClickListener(v -> openBookings("Pending"));
        findViewById(R.id.cardPAddPackage).setOnClickListener(v -> openPackages());
        findViewById(R.id.cardPUpload).setOnClickListener(v -> openBookings("Confirmed"));
        findViewById(R.id.cardPMessages).setOnClickListener(v -> openMessages());

        setupFilterChips();
        setupPackageForm();
        setupMessaging();
        setupProfile();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (currentScreen != screenHome) openHome();
                else finish();
            }
        });

        openHome();
    }

    // ---------------------------------------------------------------- navigation

    private void showScreen(View target, int navId) {
        View[] all = {screenHome, screenBookings, screenPackages, screenMessages, screenProfile};
        for (View v : all) v.setVisibility(v == target ? View.VISIBLE : View.GONE);
        currentScreen = target;
        nav.select(navId);
    }

    private void openHome() { renderDashboard(); showScreen(screenHome, R.id.navHome); }
    private void openBookings(String filter) { activeFilter = filter; renderFilterChips(); renderBookings(); showScreen(screenBookings, R.id.navBookings); }
    private void openPackages() { renderPackages(); showScreen(screenPackages, R.id.navPackages); }
    private void openMessages() { renderMessages(); showScreen(screenMessages, R.id.navMessages); }
    private void openProfile() { showScreen(screenProfile, R.id.navProfile); }

    private void refreshAll() {
        renderDashboard();
        renderBookings();
    }

    // ---------------------------------------------------------------- dashboard

    private void renderDashboard() {
        bookingRepo.getBookingsForPhotographer(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                int pending = 0, confirmed = 0;
                for (Booking b : list) {
                    if (FirestoreConstants.STATUS_PENDING.equalsIgnoreCase(b.getStatus())) pending++;
                    if (FirestoreConstants.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) confirmed++;
                }
                ((TextView) findViewById(R.id.tvStatPending)).setText(String.valueOf(pending));
                ((TextView) findViewById(R.id.tvStatUpcoming)).setText(String.valueOf(confirmed));

                pendingContainer.removeAllViews();
                int shown = 0;
                for (Booking b : list) {
                    if (FirestoreConstants.STATUS_PENDING.equalsIgnoreCase(b.getStatus())) {
                        pendingContainer.addView(buildBookingCard(b));
                        shown++;
                    }
                }
                if (shown == 0) pendingContainer.addView(UiUtils.emptyState(PhotographerActivity.this, "You're all caught up. New requests show up here."));

                nextSessionContainer.removeAllViews();
                Booking next = BookingRepository.nextUpcoming(confirmedOnly(list));
                if (next == null) {
                    nextSessionContainer.addView(UiUtils.emptyState(PhotographerActivity.this, "No confirmed sessions ahead."));
                } else {
                    nextSessionContainer.addView(buildBookingCard(next));
                }
            }
            @Override public void onError(Exception e) {}
        });
        packageRepo.getPackagesForStudio(session.getName(), new RepositoryCallback<List<Package>>() {
            @Override public void onSuccess(List<Package> list) {
                ((TextView) findViewById(R.id.tvStatPackages)).setText(String.valueOf(list.size()));
            }
            @Override public void onError(Exception e) {}
        });
    }

    private List<Booking> confirmedOnly(List<Booking> all) {
        List<Booking> res = new ArrayList<>();
        for (Booking b : all) if (FirestoreConstants.STATUS_CONFIRMED.equalsIgnoreCase(b.getStatus())) res.add(b);
        return res;
    }

    // ---------------------------------------------------------------- bookings

    private void setupFilterChips() { renderFilterChips(); }

    private void renderFilterChips() {
        filterChips.removeAllViews();
        for (String f : FILTERS) {
            boolean on = f.equals(activeFilter);
            TextView chip = UiUtils.text(this, f, 13, on ? R.color.surface_white : R.color.text_primary, on);
            chip.setBackgroundResource(on ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            chip.setPadding(UiUtils.dp(this, 16), UiUtils.dp(this, 9), UiUtils.dp(this, 16), UiUtils.dp(this, 9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(UiUtils.dp(this, 8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> { activeFilter = f; renderFilterChips(); renderBookings(); });
            filterChips.addView(chip);
        }
    }

    private void renderBookings() {
        bookingsContainer.removeAllViews();
        bookingRepo.getBookingsForPhotographer(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                int shown = 0;
                for (Booking b : list) {
                    if (!"All".equals(activeFilter) && !activeFilter.equalsIgnoreCase(b.getStatus())) continue;
                    bookingsContainer.addView(buildBookingCard(b));
                    shown++;
                }
                if (shown == 0) {
                    String msg = "All".equals(activeFilter)
                        ? "No bookings yet. Clients will appear here once they book."
                        : "No " + activeFilter.toLowerCase(Locale.ROOT) + " bookings.";
                    bookingsContainer.addView(UiUtils.emptyState(PhotographerActivity.this, msg));
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    private View buildBookingCard(Booking b) {
        LinearLayout card = UiUtils.card(this);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = UiUtils.text(this, b.getServiceTitle(), 17, R.color.text_primary, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title);
        top.addView(UiUtils.text(this, UiUtils.money(b.getTotalPrice()), 16, R.color.text_primary, true));
        card.addView(top);

        LinearLayout clientRow = new LinearLayout(this);
        clientRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams crlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        crlp.topMargin = UiUtils.dp(this, 10);
        clientRow.setLayoutParams(crlp);
        TextView avatar = UiUtils.text(this, UiUtils.initial(b.getClientName()), 13, R.color.surface_white, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_icon_circle_dark);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(UiUtils.dp(this, 28), UiUtils.dp(this, 28)));
        clientRow.addView(avatar);
        TextView name = UiUtils.text(this, b.getClientName(), 14, R.color.text_primary, true);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.setMarginStart(UiUtils.dp(this, 10));
        name.setLayoutParams(nlp);
        clientRow.addView(name);
        card.addView(clientRow);

        TextView meta = UiUtils.text(this, b.getDate() + " \u2022 " + b.getTimeSlot() + " \u2022 " + b.getLocation(), 13, R.color.text_secondary, false);
        meta.setPadding(0, UiUtils.dp(this, 8), 0, 0);
        card.addView(meta);

        final TextView photoLine = UiUtils.text(this, "", 12, R.color.text_secondary, false);
        photoLine.setVisibility(View.GONE);
        card.addView(photoLine);
        galleryRepo.getPhotosForBooking(b.getBookingId(), new RepositoryCallback<List<Photo>>() {
            @Override public void onSuccess(List<Photo> photos) {
                if (photos.isEmpty()) return;
                photoLine.setText(photos.size() + (photos.size() == 1 ? " photo delivered" : " photos delivered"));
                photoLine.setVisibility(View.VISIBLE);
            }
            @Override public void onError(Exception e) {}
        });

        TextView status = new TextView(this);
        UiUtils.styleStatus(status, b.getStatus());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = UiUtils.dp(this, 10);
        status.setLayoutParams(slp);
        card.addView(status);

        String st = b.getStatus();
        if (FirestoreConstants.STATUS_PENDING.equalsIgnoreCase(st)) {
            LinearLayout row = actionRow();
            row.addView(button(row, true, "Accept", v -> setStatus(b, FirestoreConstants.STATUS_CONFIRMED, null)));
            row.addView(button(row, false, "Decline", v -> setStatus(b, FirestoreConstants.STATUS_CANCELLED,
                "Decline this request? The slot will open up for other clients.")));
            spaceChildren(row);
            card.addView(row);
        } else if (FirestoreConstants.STATUS_CONFIRMED.equalsIgnoreCase(st)) {
            LinearLayout row = actionRow();
            row.addView(button(row, true, "Upload Photos", v -> uploadPhoto(b)));
            row.addView(button(row, false, "Mark Complete", v -> setStatus(b, FirestoreConstants.STATUS_COMPLETED, null)));
            spaceChildren(row);
            card.addView(row);
            LinearLayout row2 = actionRow();
            row2.addView(button(row2, false, "Cancel Booking", v -> setStatus(b, FirestoreConstants.STATUS_CANCELLED,
                "Cancel this booking? The client will see it as cancelled and the slot frees up.")));
            card.addView(row2);
        } else if (FirestoreConstants.STATUS_COMPLETED.equalsIgnoreCase(st)) {
            LinearLayout row = actionRow();
            row.addView(button(row, true, "Upload Photos", v -> uploadPhoto(b)));
            card.addView(row);
        }
        return card;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiUtils.dp(this, 12);
        row.setLayoutParams(lp);
        return row;
    }

    private void spaceChildren(LinearLayout row) {
        for (int i = 1; i < row.getChildCount(); i++) {
            ((LinearLayout.LayoutParams) row.getChildAt(i).getLayoutParams()).setMarginStart(UiUtils.dp(this, 10));
        }
    }

    private MaterialButton button(ViewGroup parent, boolean filled, String text, View.OnClickListener l) {
        MaterialButton b = (MaterialButton) getLayoutInflater().inflate(
            filled ? R.layout.view_action_button : R.layout.view_action_button_outline, parent, false);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private void setStatus(Booking b, String newStatus, String confirmMessage) {
        if (!Permissions.can(session.getRole(), Permissions.Feature.MANAGE_BOOKINGS)) return;
        Runnable apply = () -> bookingRepo.updateStatus(b.getBookingId(), newStatus, new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void r) {
                Toast.makeText(PhotographerActivity.this, "Booking " + newStatus.toLowerCase(Locale.ROOT) + ".", Toast.LENGTH_SHORT).show();
                refreshAll();
            }
            @Override public void onError(Exception e) {}
        });
        if (confirmMessage == null) { apply.run(); return; }
        new AlertDialog.Builder(this)
            .setMessage(confirmMessage)
            .setNegativeButton("Go back", null)
            .setPositiveButton("Yes, continue", (d, w) -> apply.run())
            .show();
    }

    private void uploadPhoto(Booking b) {
        if (!Permissions.can(session.getRole(), Permissions.Feature.UPLOAD_PHOTOS)) return;
        uploadTarget = b;
        photoPicker.launch("image/*"); // system picker: browse the gallery and select as many as you like
    }

    private void onPhotosPicked(List<Uri> uris) {
        final Booking target = uploadTarget;
        if (target == null || uris == null || uris.isEmpty()) return;

        final AlertDialog progress = new AlertDialog.Builder(this)
            .setTitle("Uploading photos")
            .setMessage("Starting\u2026")
            .setCancelable(false)
            .create();
        progress.show();

        galleryRepo.uploadPhotos(this, target.getBookingId(), session.getUserId(), uris,
            (done, total) -> {
                if (!isFinishing() && !isDestroyed()) progress.setMessage("Uploading " + (done + 1) + " of " + total + "\u2026 keep the app open.");
            },
            new RepositoryCallback<GalleryRepository.UploadResult>() {
                @Override public void onSuccess(GalleryRepository.UploadResult r) {
                    uploadTarget = null;
                    String msg = r.failed == 0
                        ? r.uploaded + (r.uploaded == 1 ? " photo" : " photos") + " delivered to " + target.getClientName() + "."
                        : r.uploaded + " uploaded, " + r.failed + " failed. Tap Upload Photos to retry the missing ones.";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    if (!isFinishing() && !isDestroyed()) {
                        progress.dismiss();
                        refreshAll();
                    }
                }
                @Override public void onError(Exception e) {
                    if (!isFinishing() && !isDestroyed()) progress.dismiss();
                    Toast.makeText(getApplicationContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    // ---------------------------------------------------------------- packages

    private void setupPackageForm() {
        EditText etName = findViewById(R.id.etPkgName);
        EditText etPrice = findViewById(R.id.etPkgPrice);
        EditText etHours = findViewById(R.id.etPkgHours);
        EditText etInc = findViewById(R.id.etPkgInclusions);

        findViewById(R.id.btnSavePackage).setOnClickListener(v -> {
            if (!Permissions.can(session.getRole(), Permissions.Feature.MANAGE_PACKAGES)) return;
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String hrsStr = etHours.getText().toString().trim();
            String inc = etInc.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Add a package title and a price.", Toast.LENGTH_SHORT).show();
                return;
            }
            double price;
            int hrs;
            try {
                price = Double.parseDouble(priceStr);
                hrs = hrsStr.isEmpty() ? 2 : Integer.parseInt(hrsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Price and hours must be numbers.", Toast.LENGTH_SHORT).show();
                return;
            }
            Package pkg = new Package(null, "srv_custom", session.getName(), name, price, hrs, inc);
            packageRepo.addPackage(pkg, new RepositoryCallback<Package>() {
                @Override public void onSuccess(Package p) {
                    Toast.makeText(PhotographerActivity.this, "Package published. Clients can book it now.", Toast.LENGTH_LONG).show();
                    etName.setText(""); etPrice.setText(""); etHours.setText(""); etInc.setText("");
                    renderPackages();
                }
                @Override public void onError(Exception e) {}
            });
        });
    }

    private void renderPackages() {
        packagesContainer.removeAllViews();
        packageRepo.getPackagesForStudio(session.getName(), new RepositoryCallback<List<Package>>() {
            @Override public void onSuccess(List<Package> list) {
                if (list.isEmpty()) {
                    packagesContainer.addView(UiUtils.emptyState(PhotographerActivity.this, "No packages yet. Publish your first one above."));
                    return;
                }
                for (Package p : list) {
                    LinearLayout card = UiUtils.card(PhotographerActivity.this);
                    LinearLayout top = new LinearLayout(PhotographerActivity.this);
                    top.setGravity(Gravity.CENTER_VERTICAL);
                    TextView title = UiUtils.text(PhotographerActivity.this, p.getName(), 16, R.color.text_primary, true);
                    title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    top.addView(title);
                    top.addView(UiUtils.text(PhotographerActivity.this, UiUtils.money(p.getPrice()), 16, R.color.text_primary, true));
                    card.addView(top);
                    card.addView(UiUtils.text(PhotographerActivity.this,
                        p.getInclusions() + " (" + p.getDurationHours() + " hrs)", 13, R.color.text_secondary, false));

                    LinearLayout row = actionRow();
                    row.addView(button(row, false, "Remove", v -> new AlertDialog.Builder(PhotographerActivity.this)
                        .setMessage("Remove \"" + p.getName() + "\"? Clients won't be able to book it any more.")
                        .setNegativeButton("Keep it", null)
                        .setPositiveButton("Remove", (d, w) -> packageRepo.removePackage(p.getPackageId(), new RepositoryCallback<Void>() {
                            @Override public void onSuccess(Void r) { renderPackages(); }
                            @Override public void onError(Exception e) {}
                        }))
                        .show()));
                    row.addView(new View(PhotographerActivity.this), new LinearLayout.LayoutParams(0, 1, 2f));
                    card.addView(row);
                    packagesContainer.addView(card);
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ---------------------------------------------------------------- messages (no AI)

    private void setupMessaging() {
        EditText et = findViewById(R.id.etChatInput);
        View.OnClickListener send = v -> {
            String text = et.getText().toString().trim();
            if (text.isEmpty()) return;
            et.setText("");
            chatRepo.sendDirectMessage(session.getRole(), text);
            renderMessages();
        };
        findViewById(R.id.btnSendChat).setOnClickListener(send);
        et.setOnEditorActionListener((tv, actionId, event) -> { send.onClick(tv); return true; });
    }

    private void renderMessages() {
        // Header: the client on the studio's most recent booking.
        final TextView avatar = findViewById(R.id.tvThreadAvatar);
        final TextView threadName = findViewById(R.id.tvThreadName);
        final TextView threadSub = findViewById(R.id.tvThreadSub);
        avatar.setText("C"); threadName.setText("Client"); threadSub.setText("Direct messages");
        bookingRepo.getBookingsForPhotographer(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                if (!list.isEmpty()) {
                    Booking latest = list.get(0);
                    avatar.setText(UiUtils.initial(latest.getClientName()));
                    threadName.setText(latest.getClientName());
                    threadSub.setText(latest.getServiceTitle());
                }
            }
            @Override public void onError(Exception e) {}
        });

        chatContainer.removeAllViews();
        for (Message m : chatRepo.getDirectMessages()) {
            boolean mine = FirestoreConstants.ROLE_PHOTOGRAPHER.equalsIgnoreCase(m.getSenderRole());
            UiUtils.addBubble(chatContainer, chatScroll, m.getText(),
                UiUtils.timeCaption(this, m, mine ? "You" : "Client"), mine);
        }
    }

    // ---------------------------------------------------------------- profile

    private void setupProfile() {
        ((TextView) findViewById(R.id.tvProfileTitle)).setText("Studio Account");
        ((TextView) findViewById(R.id.tvAvatar)).setText(UiUtils.initial(session.getName()));
        ((TextView) findViewById(R.id.tvProfileName)).setText(session.getName());
        ((TextView) findViewById(R.id.tvProfileEmail)).setText(session.getEmail());
        TextView role = findViewById(R.id.tvProfileRole);
        role.setText(Permissions.label(session.getRole()));
        role.setBackgroundResource(R.drawable.bg_toggle_container);
        role.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        role.setTextSize(12);
        role.setTypeface(null, android.graphics.Typeface.BOLD);
        role.setPadding(UiUtils.dp(this, 14), UiUtils.dp(this, 5), UiUtils.dp(this, 14), UiUtils.dp(this, 5));
        findViewById(R.id.btnLogoutProfile).setOnClickListener(v -> {
            authRepo.logout();
            session.clear();
            RoleGuard.go(this, LoginActivity.class);
        });
    }
}
