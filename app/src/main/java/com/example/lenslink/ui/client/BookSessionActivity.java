package com.example.lenslink.ui.client;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.data.BookingRepository;
import com.example.lenslink.model.AddonOffering;
import com.example.lenslink.model.PackageOffering;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.ServiceOffering;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client booking flow: choose one photographer's package/services/extras, then reserve a slot. */
public final class BookSessionActivity extends AppCompatActivity {
    private final List<PackageOffering> packages = new ArrayList<>();
    private final List<ServiceOffering> services = new ArrayList<>();
    private final List<AddonOffering> addons = new ArrayList<>();
    private final Set<String> selectedServiceIds = new LinkedHashSet<>();
    private final Set<String> selectedAddonIds = new LinkedHashSet<>();
    private AuthRepository auth;
    private FirebaseFirestore firestore;
    private LinearLayout root;
    private LinearLayout serviceChoices;
    private LinearLayout addonChoices;
    private RadioGroup packageGroup;
    private TextView status;
    private TextView total;
    private TextView dateLabel;
    private RadioGroup timeSlots;
    private TextView timeSlotMessage;
    private String selectedTime;
    private CheckBox fullDayCheck;
    private EditText locationInput;
    private PackageOffering selectedPackage;
    private String clientId;
    private String clientName;
    private String photographerId;
    private String selectedDate;
    private boolean loading;
    private List<String> availableTimes = new ArrayList<>();
    private final BookingRepository bookingRepository = new BookingRepository();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();
        firestore = FirebaseFirestore.getInstance();
        if (!auth.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }
        UiUtils.enableEdgeToEdge(this);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        root.setBackgroundColor(getColor(R.color.lenslink_cream));
        scroll.addView(root);
        setContentView(scroll);
        UiUtils.applySystemBarInsets(scroll);
        root.addView(text("Book a session", 26, true));
        root.addView(text("Choose a package or services from one photographer. You can add optional extras.", 14, false));
        status = text("Checking your account…", 14, false);
        root.addView(status);

        auth.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                if (!Role.CLIENT.equals(user.getRole())) {
                    AppNavigator.goToPhotographerHome(BookSessionActivity.this);
                    return;
                }
                clientId = auth.getCurrentUserId();
                clientName = user.getFullName();
                buildForm();
                loadOfferings();
            }
            @Override public void onProfileMissing() {
                if (!isFinishing() && !isDestroyed()) AppNavigator.goToCompleteProfile(BookSessionActivity.this);
            }
            @Override public void onError(String message) { status.setText("Could not load your account: " + message); }
        });
    }

    private void buildForm() {
        root.removeViews(1, root.getChildCount() - 1);
        addSection("Packages");
        packageGroup = new RadioGroup(this);
        packageGroup.setOrientation(RadioGroup.VERTICAL);
        root.addView(packageGroup);

        addSection("Additional services");
        serviceChoices = new LinearLayout(this);
        serviceChoices.setOrientation(LinearLayout.VERTICAL);
        root.addView(serviceChoices);

        addSection("Optional add-ons");
        addonChoices = new LinearLayout(this);
        addonChoices.setOrientation(LinearLayout.VERTICAL);
        root.addView(addonChoices);

        addSection("Date and time");
        dateLabel = text("Choose a date", 15, false);
        root.addView(dateLabel);
        MaterialButton dateButton = button("Select date");
        dateButton.setOnClickListener(v -> showDatePicker());
        root.addView(dateButton);

        addSection("Available time slots");
        timeSlotMessage = text("Select a date to see available times.", 14, false);
        root.addView(timeSlotMessage);
        timeSlots = new RadioGroup(this);
        timeSlots.setOrientation(RadioGroup.VERTICAL);
        root.addView(timeSlots, matchWrap());
        fullDayCheck = new CheckBox(this);
        fullDayCheck.setText("Reserve the full day");
        fullDayCheck.setEnabled(false);
        fullDayCheck.setOnCheckedChangeListener((button, checked) -> setTimeSlotsEnabled(!checked));
        root.addView(fullDayCheck);

        addSection("Location");
        locationInput = new EditText(this);
        locationInput.setSingleLine(true);
        locationInput.setHint("Where should the photographer meet you?");
        locationInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(locationInput, matchWrap());

        total = text("Total: R 0.00", 18, true);
        total.setPadding(0, dp(14), 0, dp(6));
        root.addView(total);
        MaterialButton submit = button("Request booking");
        submit.setOnClickListener(v -> submitBooking(submit));
        root.addView(submit);
        MaterialButton back = button("Back to dashboard");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        updateTimeSlots(new ArrayList<>());
    }

    private void loadOfferings() {
        status.setText("Loading available offerings…");
        Task<QuerySnapshot> packageTask = firestore.collection("packages").whereEqualTo("active", true).get();
        Task<QuerySnapshot> serviceTask = firestore.collection("services").whereEqualTo("active", true).get();
        Task<QuerySnapshot> addonTask = firestore.collection("addons").whereEqualTo("active", true).get();
        Tasks.whenAllSuccess(Arrays.asList(packageTask, serviceTask, addonTask))
                .addOnSuccessListener(results -> {
                    if (isFinishing() || isDestroyed()) return;
                    packages.clear();
                    services.clear();
                    addons.clear();
                    for (DocumentSnapshot document : ((QuerySnapshot) results.get(0)).getDocuments()) {
                        PackageOffering item = document.toObject(PackageOffering.class);
                        if (item != null) { item.setId(document.getId()); packages.add(item); }
                    }
                    for (DocumentSnapshot document : ((QuerySnapshot) results.get(1)).getDocuments()) {
                        ServiceOffering item = document.toObject(ServiceOffering.class);
                        if (item != null) { item.setId(document.getId()); services.add(item); }
                    }
                    for (DocumentSnapshot document : ((QuerySnapshot) results.get(2)).getDocuments()) {
                        AddonOffering item = document.toObject(AddonOffering.class);
                        if (item != null) { item.setId(document.getId()); addons.add(item); }
                    }
                    status.setText("Select a package or at least one service to continue.");
                    renderChoices();
                }).addOnFailureListener(error -> status.setText(
                        "Could not load offerings: " + (error.getLocalizedMessage() == null ? "check your connection and Firestore Rules." : error.getLocalizedMessage())));
    }

    private void renderChoices() {
        serviceChoices.removeAllViews();
        addonChoices.removeAllViews();
        packageGroup.setOnCheckedChangeListener(null);
        packageGroup.removeAllViews();

        RadioButton noPackage = new RadioButton(this);
        noPackage.setText("No package — I'll choose services");
        noPackage.setId(View.generateViewId());
        noPackage.setChecked(selectedPackage == null);
        packageGroup.addView(noPackage);
        packageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == noPackage.getId()) {
                selectedPackage = null;
                updateOwnerFromServices();
                renderChoices();
            }
        });

        List<PackageOffering> visiblePackages = new ArrayList<>();
        for (PackageOffering item : packages) {
            if (!hasOwner(item.getPhotographerId())) continue;
            if (photographerId == null || photographerId.equals(item.getPhotographerId())) visiblePackages.add(item);
        }
        for (PackageOffering item : visiblePackages) {
            RadioButton choice = new RadioButton(this);
            choice.setId(View.generateViewId());
            choice.setText(packageDisplayName(item) + "  ·  " + money(item.getPrice()));
            if (selectedPackage != null && selectedPackage.getId().equals(item.getId())) choice.setChecked(true);
            packageGroup.addView(choice);
            choice.setOnClickListener(v -> choosePackage(item));
        }

        boolean hasService = false;
        for (ServiceOffering item : services) {
            if (!hasOwner(item.getPhotographerId())) continue;
            if (photographerId != null && !photographerId.equals(item.getPhotographerId())) continue;
            hasService = true;
            CheckBox choice = new CheckBox(this);
            choice.setText(item.getName() + "  ·  " + money(item.getPrice()));
            choice.setChecked(selectedServiceIds.contains(item.getId()));
            serviceChoices.addView(choice);
            addDescription(serviceChoices, item.getDescription());
            choice.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    if (photographerId == null) photographerId = item.getPhotographerId();
                    selectedServiceIds.add(item.getId());
                } else {
                    selectedServiceIds.remove(item.getId());
                    updateOwnerFromServices();
                }
                renderChoices();
                updateTotal();
            });
        }
        if (!hasService) serviceChoices.addView(text("No additional services are available from this photographer.", 13, false));

        boolean hasAddon = false;
        for (AddonOffering item : addons) {
            if (!hasOwner(item.getPhotographerId())) continue;
            if (photographerId == null || !photographerId.equals(item.getPhotographerId())) continue;
            hasAddon = true;
            CheckBox choice = new CheckBox(this);
            choice.setText(item.getName() + "  ·  " + money(item.getPrice()));
            choice.setChecked(selectedAddonIds.contains(item.getId()));
            addonChoices.addView(choice);
            addDescription(addonChoices, item.getDescription());
            choice.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selectedAddonIds.add(item.getId()); else selectedAddonIds.remove(item.getId());
                updateTotal();
            });
        }
        if (!hasAddon) addonChoices.addView(text(photographerId == null
                ? "Choose a package or service first to see that photographer's add-ons."
                : "No add-ons are available for this photographer.", 13, false));
        updateTotal();
    }

    private void choosePackage(PackageOffering item) {
        if (selectedPackage != null && selectedPackage.getId().equals(item.getId())) return;
        selectedPackage = item;
        photographerId = item.getPhotographerId();
        selectedServiceIds.clear();
        selectedAddonIds.clear();
        renderChoices();
    }

    private void updateOwnerFromServices() {
        if (selectedPackage != null) {
            photographerId = selectedPackage.getPhotographerId();
            return;
        }
        photographerId = null;
        for (ServiceOffering item : services) {
            if (selectedServiceIds.contains(item.getId())) {
                photographerId = item.getPhotographerId();
                break;
            }
        }
        if (photographerId == null) selectedAddonIds.clear();
        else {
            String owner = photographerId;
            selectedAddonIds.removeIf(id -> addons.stream().noneMatch(addon -> id.equals(addon.getId())
                    && owner.equals(addon.getPhotographerId())));
        }
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            dateLabel.setText(selectedDate);
            selectedTime = null;
            availableTimes = new ArrayList<>();
            updateTimeSlots(availableTimes);
            fullDayCheck.setChecked(false);
            timeSlotMessage.setText("Checking available times…");
            loadAvailableTimes(selectedDate);
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(today.getTimeInMillis());
        picker.show();
    }

    private void loadAvailableTimes(String date) {
        firestore.collection("daily_schedules").document(date).get().addOnSuccessListener(snapshot -> {
            List<String> booked = new ArrayList<>();
            Object raw = snapshot.get("bookedSlots");
            if (raw instanceof List<?>) for (Object slot : (List<?>) raw) if (slot != null) booked.add(String.valueOf(slot));
            List<String> open = new ArrayList<>();
            if (!booked.contains("FULL_DAY")) {
                for (String slot : Arrays.asList("09:00", "11:30", "14:00", "16:30")) {
                    if (!booked.contains(slot)) open.add(slot);
                }
            }
            availableTimes = open;
            selectedTime = null;
            updateTimeSlots(open);
            fullDayCheck.setEnabled(booked.isEmpty());
            if (!booked.isEmpty()) fullDayCheck.setChecked(false);
            timeSlotMessage.setText(open.isEmpty()
                    ? "No time slots are available on this date. Please choose another date."
                    : "Tap a time to select it.");
        }).addOnFailureListener(error -> {
            availableTimes = new ArrayList<>();
            selectedTime = null;
            updateTimeSlots(availableTimes);
            timeSlotMessage.setText("Could not load times. Check your connection and try again.");
            status.setText("Could not check availability: " + error.getLocalizedMessage());
        });
    }

    private void updateTimeSlots(List<String> slots) {
        timeSlots.removeAllViews();
        selectedTime = null;
        for (String slot : slots) {
            RadioButton choice = new RadioButton(this);
            choice.setId(View.generateViewId());
            choice.setText(slot);
            timeSlots.addView(choice);
            choice.setOnClickListener(v -> selectedTime = slot);
        }
        setTimeSlotsEnabled(!fullDayCheck.isChecked());
    }

    private void setTimeSlotsEnabled(boolean enabled) {
        if (timeSlots == null) return;
        for (int i = 0; i < timeSlots.getChildCount(); i++) {
            timeSlots.getChildAt(i).setEnabled(enabled && !availableTimes.isEmpty());
        }
    }

    private void submitBooking(MaterialButton submit) {
        if (loading) return;
        if (photographerId == null || (selectedPackage == null && selectedServiceIds.isEmpty())) {
            toast("Choose a package or at least one service first.");
            return;
        }
        if (selectedDate == null) {
            toast("Choose a date for your session.");
            return;
        }
        boolean fullDay = fullDayCheck.isChecked();
        if (!fullDay && (availableTimes.isEmpty() || selectedTime == null
                || !availableTimes.contains(selectedTime))) {
            toast("Select one of the available time slots.");
            return;
        }
        String location = locationInput.getText().toString().trim();
        if (location.isEmpty()) {
            locationInput.setError("Enter a location");
            return;
        }
        String start = fullDay ? null : selectedTime;
        String end = fullDay ? null : calculateEndTime(start);
        List<String> serviceIds = new ArrayList<>(selectedServiceIds);
        List<String> addonIds = new ArrayList<>(selectedAddonIds);
        loading = true;
        submit.setEnabled(false);
        submit.setText("Submitting…");
        try {
            bookingRepository.createBooking(clientId, photographerId,
                    selectedPackage == null ? null : selectedPackage.getId(),
                    selectedPackage == null ? null : packageDisplayName(selectedPackage),
                    serviceIds, selectedServiceNames(), addonIds, selectedAddonNames(), clientName,
                    selectedDate, start, end, fullDay, location, calculateTotal())
                    .addOnSuccessListener(id -> {
                        if (isFinishing() || isDestroyed()) return;
                        toast("Booking request sent. Reference: " + id);
                        finish();
                    }).addOnFailureListener(error -> {
                        loading = false;
                        submit.setEnabled(true);
                        submit.setText("Request booking");
                        String message = error.getLocalizedMessage();
                        status.setText(message == null ? "Booking could not be saved. Check Firestore Rules and try again." : message);
                    });
        } catch (IllegalArgumentException error) {
            loading = false;
            submit.setEnabled(true);
            submit.setText("Request booking");
            toast(error.getMessage());
        }
    }

    private String packageDisplayName(PackageOffering item) {
        String category = item.getCategory();
        String name = item.getName();
        if (category == null || category.trim().isEmpty()) return name;
        return category + " · " + name;
    }

    private List<String> selectedServiceNames() {
        List<String> names = new ArrayList<>();
        for (ServiceOffering item : services) {
            if (selectedServiceIds.contains(item.getId())) names.add(item.getName());
        }
        return names;
    }

    private List<String> selectedAddonNames() {
        List<String> names = new ArrayList<>();
        for (AddonOffering item : addons) {
            if (selectedAddonIds.contains(item.getId())) names.add(item.getName());
        }
        return names;
    }
    private String calculateEndTime(String start) {
        long minutes = 0;
        for (ServiceOffering item : services) if (selectedServiceIds.contains(item.getId())) minutes += item.getDurationMinutes();
        if (selectedPackage != null) minutes += 120;
        if (minutes <= 0) minutes = 60;
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
        format.setLenient(false);
        try {
            Date parsed = format.parse(start);
            if (parsed == null) return start;
            Calendar end = Calendar.getInstance();
            end.setTime(parsed);
            end.add(Calendar.MINUTE, (int) Math.min(minutes, Integer.MAX_VALUE));
            return format.format(end.getTime());
        } catch (ParseException error) {
            return start;
        }
    }

    private double calculateTotal() {
        double amount = selectedPackage == null ? 0 : selectedPackage.getPrice();
        for (ServiceOffering item : services) if (selectedServiceIds.contains(item.getId())) amount += item.getPrice();
        for (AddonOffering item : addons) if (selectedAddonIds.contains(item.getId())) amount += item.getPrice();
        return amount;
    }

    private void updateTotal() { if (total != null) total.setText("Total: " + money(calculateTotal())); }
    private boolean hasOwner(String value) { return value != null && !value.trim().isEmpty(); }
    private String money(double amount) { return String.format(Locale.getDefault(), "R %.2f", amount); }
    private void addSection(String title) { root.addView(text(title, 19, true)); }
    private void addDescription(LinearLayout parent, String description) {
        if (description != null && !description.trim().isEmpty()) parent.addView(text(description, 13, false));
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
    private MaterialButton button(String value) {
        MaterialButton button = new MaterialButton(this);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
