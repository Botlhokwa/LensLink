package com.example.lenslink;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Client experience: home, services, bookings, AI assistant + photographer messaging, profile. */
public class MainActivity extends AppCompatActivity {
    private static final String[] SLOTS = {"09:00 AM", "11:30 AM", "02:00 PM", "04:30 PM"};

    private final PackageRepository packageRepo = new PackageRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final GalleryRepository galleryRepo = new GalleryRepository();
    private final ChatRepository chatRepo = new ChatRepository();
    private final AuthRepository authRepo = new AuthRepository();
    private SessionManager session;

    private View screenHome, screenServices, screenBookings, screenChat, screenBookSession, screenProfile;
    private View currentScreen;
    private BottomNavBar nav;
    private LinearLayout servicesContainer, bookingsContainer, chatMessagesContainer, slotsContainer, chipsContainer;
    private ScrollView chatScroll;
    private Spinner spinnerPhotographer, spinnerPackage;
    private EditText etLocation;
    private TextView tvSummaryPackage, tvSummaryWhen, tvSummaryTotal;
    private TextView tabAi, tabDirect;

    private final Map<String, List<Package>> packagesByStudio = new LinkedHashMap<>();
    private boolean suppressSpinnerEvents = false;
    private String selectedDate;
    private String selectedSlot = null;
    private boolean isAiChatMode = true;
    private final List<Message> aiThread = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!RoleGuard.enforce(this, session, FirestoreConstants.ROLE_CLIENT)) return;
        setContentView(R.layout.activity_main);

        nav = findViewById(R.id.bottomNav);
        SystemBars.apply(this, findViewById(R.id.root), nav);

        screenHome = findViewById(R.id.screenHome);
        screenServices = findViewById(R.id.screenServices);
        screenBookings = findViewById(R.id.screenBookings);
        screenChat = findViewById(R.id.screenChat);
        screenBookSession = findViewById(R.id.screenBookSession);
        screenProfile = findViewById(R.id.screenProfile);
        servicesContainer = findViewById(R.id.servicesListContainer);
        bookingsContainer = findViewById(R.id.bookingsListContainer);
        chatMessagesContainer = findViewById(R.id.chatMessagesContainer);
        chatScroll = findViewById(R.id.chatScroll);

        ((TextView) findViewById(R.id.tvGreetingName)).setText(session.getName());
        findViewById(R.id.btnNotificationBell).setOnClickListener(v -> showUpcomingReminder());

        findViewById(R.id.cardBrowseServices).setOnClickListener(v -> openServices());
        findViewById(R.id.cardBookSession).setOnClickListener(v -> openBookSession(null));
        findViewById(R.id.cardMyBookings).setOnClickListener(v -> openBookings());
        findViewById(R.id.cardChatAssistant).setOnClickListener(v -> openChat());
        findViewById(R.id.upcomingSessionCard).setOnClickListener(v -> openBookings());
        findViewById(R.id.btnBackServices).setOnClickListener(v -> openHome());
        findViewById(R.id.btnBackBook).setOnClickListener(v -> openHome());

        // Role-shaped navigation: the client gets the AI chat tab, the photographer does not.
        List<BottomNavBar.Item> items = new ArrayList<>();
        items.add(new BottomNavBar.Item(R.id.navHome, R.drawable.ic_home, "Home"));
        items.add(new BottomNavBar.Item(R.id.navBookings, R.drawable.ic_list, "Bookings"));
        items.add(new BottomNavBar.Item(R.id.navChat, R.drawable.ic_chat, "Chat"));
        items.add(new BottomNavBar.Item(R.id.navProfile, R.drawable.ic_person, "Profile"));
        nav.setItems(items);
        nav.setOnItemSelectedListener(id -> {
            if (id == R.id.navHome) openHome();
            else if (id == R.id.navBookings) openBookings();
            else if (id == R.id.navChat) openChat();
            else if (id == R.id.navProfile) openProfile();
        });

        setupProfile();
        setupBookSessionScreen();
        setupChatScreen();

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
        View[] all = {screenHome, screenServices, screenBookings, screenChat, screenBookSession, screenProfile};
        for (View v : all) v.setVisibility(v == target ? View.VISIBLE : View.GONE);
        currentScreen = target;
        nav.select(navId);
    }

    private void openHome() { renderUpcoming(); showScreen(screenHome, R.id.navHome); }
    private void openServices() { renderBrowseServices(); showScreen(screenServices, R.id.navHome); }
    private void openBookings() { renderClientBookings(); showScreen(screenBookings, R.id.navBookings); }
    private void openProfile() { showScreen(screenProfile, R.id.navProfile); }

    private void openChat() {
        if (!Permissions.can(session.getRole(), Permissions.Feature.AI_CHAT)) return;
        renderChatMessages();
        showScreen(screenChat, R.id.navChat);
    }

    private void openBookSession(Package preselected) {
        loadPackages(preselected);
        showScreen(screenBookSession, R.id.navHome);
    }

    // ---------------------------------------------------------------- home

    private void renderUpcoming() {
        bookingRepo.getBookingsForClient(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                Booking next = BookingRepository.nextUpcoming(list);
                View content = findViewById(R.id.upcomingContent);
                View empty = findViewById(R.id.tvUpEmpty);
                if (next == null) {
                    content.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                    return;
                }
                content.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
                ((TextView) findViewById(R.id.tvUpCategory)).setText(UiUtils.categoryFor(next.getServiceTitle()));
                UiUtils.styleStatus((TextView) findViewById(R.id.tvUpStatus), next.getStatus());
                ((TextView) findViewById(R.id.tvUpPrice)).setText(UiUtils.money(next.getTotalPrice()));
                ((TextView) findViewById(R.id.tvUpTitle)).setText(next.getServiceTitle());
                ((TextView) findViewById(R.id.tvUpPhotographer)).setText("With " + next.getPhotographerName());
                ((TextView) findViewById(R.id.tvUpWhen)).setText(prettyWhen(next) + " \u2022 " + next.getLocation());
            }
            @Override public void onError(Exception e) {}
        });
    }

    private String prettyWhen(Booking b) {
        long t = BookingRepository.whenMillis(b);
        if (t < 0) return b.getDate() + ", " + b.getTimeSlot();
        return new SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).format(new Date(t));
    }

    private void showUpcomingReminder() {
        bookingRepo.getBookingsForClient(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                Booking next = BookingRepository.nextUpcoming(list);
                if (next == null) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("No upcoming sessions")
                        .setMessage("When you book a session it will show up here.")
                        .setPositiveButton("OK", null).show();
                    return;
                }
                String text = next.getServiceTitle() + " with " + next.getPhotographerName()
                    + " is coming up on " + prettyWhen(next) + ".";
                String channelId = "lenslink_sessions";
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
                    nm.createNotificationChannel(new NotificationChannel(channelId, "Upcoming Sessions", NotificationManager.IMPORTANCE_HIGH));
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, channelId)
                    .setSmallIcon(R.drawable.ic_bell)
                    .setContentTitle("Upcoming session")
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                try { if (nm != null) nm.notify(101, builder.build()); } catch (SecurityException ignored) {}

                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Upcoming session")
                    .setMessage(next.getServiceTitle() + "\n" + next.getPhotographerName() + "\n"
                        + prettyWhen(next) + "\n" + next.getLocation())
                    .setPositiveButton("Got it", null).show();
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ---------------------------------------------------------------- services

    private void renderBrowseServices() {
        servicesContainer.removeAllViews();
        packageRepo.getAllPackages(new RepositoryCallback<List<Package>>() {
            @Override public void onSuccess(List<Package> list) {
                if (list.isEmpty()) {
                    servicesContainer.addView(UiUtils.emptyState(MainActivity.this, "No packages are published yet. Check back soon."));
                    return;
                }
                for (Package p : list) {
                    LinearLayout card = UiUtils.card(MainActivity.this);
                    card.addView(UiUtils.text(MainActivity.this, p.getPhotographerName(), 12, R.color.text_secondary, true));

                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    TextView title = UiUtils.text(MainActivity.this, p.getName(), 17, R.color.text_primary, true);
                    title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(title);
                    row.addView(UiUtils.text(MainActivity.this, UiUtils.money(p.getPrice()), 16, R.color.text_primary, true));
                    row.setPadding(0, UiUtils.dp(MainActivity.this, 4), 0, UiUtils.dp(MainActivity.this, 4));
                    card.addView(row);

                    card.addView(UiUtils.text(MainActivity.this, p.getInclusions() + " (" + p.getDurationHours() + " hrs)", 13, R.color.text_secondary, false));

                    MaterialButton book = (MaterialButton) LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.view_action_button, card, false);
                    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    blp.topMargin = UiUtils.dp(MainActivity.this, 14);
                    book.setLayoutParams(blp);
                    book.setText("Book This Photographer");
                    book.setOnClickListener(v -> openBookSession(p));
                    card.addView(book);
                    servicesContainer.addView(card);
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    // ---------------------------------------------------------------- bookings

    private void renderClientBookings() {
        bookingsContainer.removeAllViews();
        bookingRepo.getBookingsForClient(session.getName(), new RepositoryCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> list) {
                if (list.isEmpty()) {
                    bookingsContainer.addView(UiUtils.emptyState(MainActivity.this, "No bookings yet. Book a session to get started."));
                    return;
                }
                for (Booking b : list) bookingsContainer.addView(buildBookingCard(b));
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

        card.addView(UiUtils.text(this, b.getPhotographerName(), 13, R.color.text_secondary, false));
        TextView meta = UiUtils.text(this, b.getDate() + " \u2022 " + b.getTimeSlot() + " \u2022 " + b.getLocation(), 13, R.color.text_secondary, false);
        card.addView(meta);

        TextView status = new TextView(this);
        UiUtils.styleStatus(status, b.getStatus());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = UiUtils.dp(this, 10);
        status.setLayoutParams(slp);
        card.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = UiUtils.dp(this, 14);
        row.setLayoutParams(rlp);

        MaterialButton download = (MaterialButton) LayoutInflater.from(this).inflate(R.layout.view_action_button, row, false);
        download.setText("Download All");
        download.setOnClickListener(v -> downloadAllPhotos(b));
        row.addView(download);

        boolean cancelled = FirestoreConstants.STATUS_CANCELLED.equalsIgnoreCase(b.getStatus());
        boolean completed = FirestoreConstants.STATUS_COMPLETED.equalsIgnoreCase(b.getStatus());
        if (!cancelled && !completed && Permissions.can(session.getRole(), Permissions.Feature.CANCEL_OWN_BOOKING)) {
            MaterialButton cancel = (MaterialButton) LayoutInflater.from(this).inflate(R.layout.view_action_button_outline, row, false);
            ((LinearLayout.LayoutParams) cancel.getLayoutParams()).setMarginStart(UiUtils.dp(this, 10));
            cancel.setText("Cancel");
            cancel.setOnClickListener(v -> confirmCancel(b));
            row.addView(cancel);
        }
        card.addView(row);
        return card;
    }

    private void confirmCancel(Booking b) {
        new AlertDialog.Builder(this)
            .setTitle("Cancel this booking?")
            .setMessage("The " + b.getTimeSlot() + " slot on " + b.getDate() + " will open up for other clients.")
            .setNegativeButton("Keep booking", null)
            .setPositiveButton("Cancel booking", (d, w) ->
                bookingRepo.updateStatus(b.getBookingId(), FirestoreConstants.STATUS_CANCELLED, new RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void r) {
                        Toast.makeText(MainActivity.this, "Booking cancelled. That slot is available again.", Toast.LENGTH_LONG).show();
                        renderClientBookings();
                    }
                    @Override public void onError(Exception e) {}
                }))
            .show();
    }

    private void downloadAllPhotos(Booking b) {
        if (!Permissions.can(session.getRole(), Permissions.Feature.DOWNLOAD_PHOTOS)) return;
        galleryRepo.getPhotosForBooking(b.getBookingId(), new RepositoryCallback<List<Photo>>() {
            @Override public void onSuccess(List<Photo> photos) {
                if (photos.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No photos yet. Your photographer will upload them after the shoot.", Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Download " + photos.size() + (photos.size() == 1 ? " photo?" : " photos?"))
                    .setMessage("They'll be saved to Pictures/LensLink/" + b.getServiceTitle()
                        + " on your phone, ready to sort in your gallery. Original quality files can be large, so Wi-Fi is best.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Download all", (d, w) -> {
                        int n = galleryRepo.downloadAll(MainActivity.this, photos, b.getServiceTitle());
                        Toast.makeText(MainActivity.this, "Downloading " + n + (n == 1 ? " photo" : " photos") + "\u2026 check your notifications for progress.", Toast.LENGTH_LONG).show();
                    })
                    .show();
            }
            @Override public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Couldn't load your photos. Check your connection and try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------------------------------------------------------- book a session

    private void setupBookSessionScreen() {
        spinnerPhotographer = findViewById(R.id.spinnerPhotographer);
        spinnerPackage = findViewById(R.id.spinnerPackage);
        slotsContainer = findViewById(R.id.slotsContainer);
        etLocation = findViewById(R.id.etBookingLocation);
        tvSummaryPackage = findViewById(R.id.tvSummaryPackage);
        tvSummaryWhen = findViewById(R.id.tvSummaryWhen);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        CalendarView calendarView = findViewById(R.id.calendarView);

        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(calendarView.getDate()));
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
            selectedSlot = null;
            refreshSlots();
            updateSummary();
        });

        spinnerPhotographer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerEvents) return;
                populatePackageSpinner(null);
                selectedSlot = null;
                refreshSlots();
                updateSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerPackage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!suppressSpinnerEvents) updateSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnConfirmBooking).setOnClickListener(v -> confirmBooking());
    }

    private void loadPackages(Package preselected) {
        packageRepo.getAllPackages(new RepositoryCallback<List<Package>>() {
            @Override public void onSuccess(List<Package> list) {
                packagesByStudio.clear();
                for (Package p : list) {
                    List<Package> bucket = packagesByStudio.get(p.getPhotographerName());
                    if (bucket == null) {
                        bucket = new ArrayList<>();
                        packagesByStudio.put(p.getPhotographerName(), bucket);
                    }
                    bucket.add(p);
                }
                suppressSpinnerEvents = true;
                spinnerPhotographer.setAdapter(spinnerAdapter(new ArrayList<>(packagesByStudio.keySet())));
                populatePackageSpinner(null);
                suppressSpinnerEvents = false;
                if (preselected != null) selectPackage(preselected);
                selectedSlot = null;
                refreshSlots();
                updateSummary();
            }
            @Override public void onError(Exception e) {}
        });
    }

    private ArrayAdapter<String> spinnerAdapter(List<String> labels) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.item_spinner, labels);
        a.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return a;
    }

    private void populatePackageSpinner(Package keep) {
        boolean prev = suppressSpinnerEvents;
        suppressSpinnerEvents = true;
        String studio = currentStudio();
        List<Package> pkgs = new ArrayList<>();
        if (studio != null && packagesByStudio.get(studio) != null) pkgs = packagesByStudio.get(studio);
        List<String> labels = new ArrayList<>();
        int keepIdx = 0;
        for (int i = 0; i < pkgs.size(); i++) {
            labels.add(pkgs.get(i).getName() + " (" + UiUtils.money(pkgs.get(i).getPrice()) + ")");
            if (keep != null && pkgs.get(i).getPackageId() != null && pkgs.get(i).getPackageId().equals(keep.getPackageId())) keepIdx = i;
        }
        spinnerPackage.setAdapter(spinnerAdapter(labels));
        if (!labels.isEmpty()) spinnerPackage.setSelection(keepIdx);
        suppressSpinnerEvents = prev;
    }

    private void selectPackage(Package p) {
        suppressSpinnerEvents = true;
        List<String> studios = new ArrayList<>(packagesByStudio.keySet());
        int idx = studios.indexOf(p.getPhotographerName());
        if (idx >= 0) spinnerPhotographer.setSelection(idx);
        populatePackageSpinner(p);
        suppressSpinnerEvents = false;
        selectedSlot = null;
    }

    private String currentStudio() {
        Object o = spinnerPhotographer.getSelectedItem();
        return o == null ? null : o.toString();
    }

    private Package currentPackage() {
        String studio = currentStudio();
        int pos = spinnerPackage.getSelectedItemPosition();
        if (studio == null || pos < 0) return null;
        List<Package> pkgs = packagesByStudio.get(studio);
        return pkgs != null && pos < pkgs.size() ? pkgs.get(pos) : null;
    }

    private void refreshSlots() {
        slotsContainer.removeAllViews();
        String studio = currentStudio();
        Set<String> locked = new java.util.HashSet<>();
        if (studio != null) locked = bookingRepo.getLockedSlots(studio, selectedDate);
        if (selectedSlot != null && locked.contains(selectedSlot)) selectedSlot = null;

        for (int i = 0; i < SLOTS.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = UiUtils.dp(this, 10);
            row.setLayoutParams(rlp);
            for (int j = i; j < i + 2; j++) {
                if (j < SLOTS.length) {
                    row.addView(buildSlot(SLOTS[j], locked.contains(SLOTS[j]), j % 2 == 0));
                } else {
                    View spacer = new View(this);
                    spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                    row.addView(spacer);
                }
            }
            slotsContainer.addView(row);
        }
    }

    private TextView buildSlot(String slot, boolean isLocked, boolean isLeft) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, UiUtils.dp(this, 48), 1f);
        int gap = UiUtils.dp(this, 5);
        lp.setMargins(isLeft ? 0 : gap, 0, isLeft ? gap : 0, 0);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(15);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        boolean selected = slot.equals(selectedSlot);
        if (isLocked) {
            tv.setText(slot + "  \u00B7  Booked");
            tv.setTextSize(13);
            tv.setBackgroundResource(R.drawable.bg_slot_booked);
            tv.setTextColor(ContextCompat.getColor(this, R.color.slot_booked_text));
            UiUtils.strike(tv, true);
        } else {
            tv.setText(slot);
            tv.setBackgroundResource(selected ? R.drawable.bg_slot_selected : R.drawable.bg_slot_available);
            tv.setTextColor(ContextCompat.getColor(this, selected ? R.color.surface_white : R.color.text_primary));
            tv.setOnClickListener(v -> { selectedSlot = slot; refreshSlots(); updateSummary(); });
        }
        return tv;
    }

    private void updateSummary() {
        Package p = currentPackage();
        if (p == null) {
            tvSummaryPackage.setText("No package selected");
            tvSummaryWhen.setText("");
            tvSummaryTotal.setText("");
            return;
        }
        tvSummaryPackage.setText(p.getName() + " with " + p.getPhotographerName());
        tvSummaryWhen.setText(selectedDate + " \u2022 " + (selectedSlot == null ? "Pick a time slot" : selectedSlot));
        tvSummaryTotal.setText(UiUtils.money(p.getPrice()));
    }

    private void confirmBooking() {
        if (!Permissions.can(session.getRole(), Permissions.Feature.BOOK_SESSION)) return;
        Package pkg = currentPackage();
        if (pkg == null) { Toast.makeText(this, "Choose a package first.", Toast.LENGTH_SHORT).show(); return; }
        if (selectedSlot == null) { Toast.makeText(this, "Pick a time slot.", Toast.LENGTH_SHORT).show(); return; }
        String loc = etLocation.getText().toString().trim();
        if (loc.isEmpty()) { Toast.makeText(this, "Add a location for the shoot.", Toast.LENGTH_SHORT).show(); return; }

        Booking nb = new Booking(null, session.getUserId(), session.getName(), pkg.getPhotographerName(), pkg.getPhotographerName(),
            pkg.getName(), selectedDate, selectedSlot, loc, FirestoreConstants.STATUS_PENDING, pkg.getPrice());
        final String slot = selectedSlot;
        bookingRepo.createBooking(nb, new RepositoryCallback<Booking>() {
            @Override public void onSuccess(Booking b) {
                Toast.makeText(MainActivity.this, "Request sent. " + slot + " is held until the photographer confirms.", Toast.LENGTH_LONG).show();
                selectedSlot = null;
                etLocation.setText("");
                openBookings();
            }
            @Override public void onError(Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                refreshSlots();
            }
        });
    }

    // ---------------------------------------------------------------- chat (AI + photographer)

    private void setupChatScreen() {
        tabAi = findViewById(R.id.tabAiAssistant);
        tabDirect = findViewById(R.id.tabDirectPhotographer);
        chipsContainer = findViewById(R.id.chipsContainer);
        EditText etChat = findViewById(R.id.etChatInput);

        tabAi.setOnClickListener(v -> setChatMode(true));
        tabDirect.setOnClickListener(v -> setChatMode(false));

        String[] chips = {"Standard Package Info", "How to Download Photos?", "Slot locking rules", "Graduation prices"};
        for (String label : chips) {
            TextView chip = UiUtils.text(this, label, 13, R.color.text_primary, false);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(UiUtils.dp(this, 14), UiUtils.dp(this, 9), UiUtils.dp(this, 14), UiUtils.dp(this, 9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(UiUtils.dp(this, 8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> handleChatSend(label));
            chipsContainer.addView(chip);
        }

        findViewById(R.id.btnSendChat).setOnClickListener(v -> sendFromInput(etChat));
        etChat.setOnEditorActionListener((tv, actionId, event) -> { sendFromInput(etChat); return true; });
    }

    private void sendFromInput(EditText et) {
        String msg = et.getText().toString().trim();
        if (!msg.isEmpty()) {
            et.setText("");
            handleChatSend(msg);
        }
    }

    private void setChatMode(boolean ai) {
        isAiChatMode = ai;
        tabAi.setBackgroundResource(ai ? R.drawable.bg_pill_dark : 0);
        tabAi.setTextColor(ContextCompat.getColor(this, ai ? R.color.surface_white : R.color.text_secondary));
        tabDirect.setBackgroundResource(!ai ? R.drawable.bg_pill_dark : 0);
        tabDirect.setTextColor(ContextCompat.getColor(this, !ai ? R.color.surface_white : R.color.text_secondary));
        findViewById(R.id.chipsScroll).setVisibility(ai ? View.VISIBLE : View.GONE);
        renderChatMessages();
    }

    private void renderChatMessages() {
        chatMessagesContainer.removeAllViews();
        if (isAiChatMode) {
            if (aiThread.isEmpty()) {
                aiThread.add(new Message("ai0", "ai_chat", "assistant", "assistant",
                    "Hello! Welcome to LensLink Assistant. Ask me about photographer packages, slot locking rules, or how to download your gallery photos.", true));
            }
            for (Message m : aiThread) {
                UiUtils.addBubble(chatMessagesContainer, chatScroll, m.getText(), null, !m.isBot());
            }
        } else {
            for (Message m : chatRepo.getDirectMessages()) {
                boolean mine = FirestoreConstants.ROLE_CLIENT.equalsIgnoreCase(m.getSenderRole());
                UiUtils.addBubble(chatMessagesContainer, chatScroll, m.getText(),
                    UiUtils.timeCaption(this, m, mine ? "You" : "Photographer"), mine);
            }
        }
    }

    private void handleChatSend(String text) {
        if (isAiChatMode) {
            aiThread.add(new Message("u" + System.nanoTime(), "ai_chat", session.getUserId(), session.getRole(), text, false));
            aiThread.add(new Message("a" + System.nanoTime(), "ai_chat", "assistant", "assistant",
                chatRepo.getTailoredAiReply(session.getRole(), text), true));
        } else {
            chatRepo.sendDirectMessage(session.getRole(), text);
        }
        renderChatMessages();
    }

    // ---------------------------------------------------------------- profile

    private void setupProfile() {
        ((TextView) findViewById(R.id.tvProfileTitle)).setText("Client Account");
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
