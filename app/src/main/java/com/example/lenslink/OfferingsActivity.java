package com.example.lenslink;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lenslink.data.AddonRepository;
import com.example.lenslink.data.BaseOfferingRepository;
import com.example.lenslink.data.CatalogSeeder;
import com.example.lenslink.data.PackageRepository;
import com.example.lenslink.data.ServiceRepository;
import com.example.lenslink.model.AddonOffering;
import com.example.lenslink.model.PackageOffering;
import com.example.lenslink.model.ServiceOffering;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client catalogue and photographer's own offering manager backed by Cloud Firestore. */
public class OfferingsActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "offering_mode";
    public static final String MODE_MANAGE = "manage";
    public static final String MODE_BROWSE = "browse";
    private static final int SERVICES = 0;
    private static final int PACKAGES = 1;
    private static final int ADDONS = 2;

    private int selectedType = SERVICES;
    private boolean managementMode;
    private boolean photographerAccount;
    private boolean profileLoaded;
    private String currentUid;
    private ListenerRegistration activeListener;
    private EditText photographerIdInput;
    private LinearLayout root;
    private LinearLayout offeringList;
    private TextView statusText;
    private MaterialButton manageButton;
    private MaterialButton browseButton;
    private MaterialButton addButton;
    private MaterialButton loadButton;
    private MaterialButton importCatalogButton;
    private TextView ownershipNote;
    private final List<MaterialButton> typeButtons = new ArrayList<>();

    private final ServiceRepository serviceRepository = new ServiceRepository();
    private final PackageRepository packageRepository = new PackageRepository();
    private final AddonRepository addonRepository = new AddonRepository();
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!authRepository.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }
        EdgeToEdge.enable(this);
        buildScreen();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(dp(20) + bars.left, dp(18) + bars.top,
                    dp(20) + bars.right, dp(18) + bars.bottom);
            return insets;
        });
        statusText.setText("Loading your account…");
        authRepository.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                currentUid = authRepository.getCurrentUserId();
                photographerAccount = Role.PHOTOGRAPHER.equals(user.getRole());
                profileLoaded = true;
                photographerIdInput.setText(currentUid == null ? "" : currentUid);
                managementMode = photographerAccount
                        && MODE_MANAGE.equals(getIntent().getStringExtra(EXTRA_MODE));
                refreshScreen();
            }
            @Override public void onProfileMissing() {
                if (!isFinishing() && !isDestroyed()) AppNavigator.goToCompleteProfile(OfferingsActivity.this);
            }
            @Override public void onError(String message) {
                if (!isFinishing() && !isDestroyed()) statusText.setText("Could not load your account: " + message);
            }
        });
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        root.setBackgroundColor(getColor(R.color.lenslink_cream));
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("Barred", 30, true);
        title.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        root.addView(title);
        TextView subtitle = text("Photography services, packages and extras", 15, false);
        subtitle.setPadding(0, 0, 0, dp(14));
        root.addView(subtitle);

        LinearLayout modeRow = row();
        browseButton = button("Browse");
        manageButton = button("Photographer management");
        modeRow.addView(browseButton, weightedParams());
        modeRow.addView(manageButton, weightedParams());
        root.addView(modeRow);
        browseButton.setOnClickListener(view -> {
            managementMode = false;
            refreshScreen();
        });
        manageButton.setOnClickListener(view -> {
            if (!photographerAccount) return;
            managementMode = true;
            refreshScreen();
        });

        TextView collectionHeading = text("Offerings", 21, true);
        collectionHeading.setPadding(0, dp(18), 0, dp(8));
        root.addView(collectionHeading);

        LinearLayout typeRow = row();
        String[] typeLabels = {"Services", "Packages", "Add-ons"};
        for (int i = 0; i < typeLabels.length; i++) {
            final int type = i;
            MaterialButton tab = button(typeLabels[i]);
            typeButtons.add(tab);
            typeRow.addView(tab, weightedParams());
            tab.setOnClickListener(view -> {
                selectedType = type;
                refreshScreen();
            });
        }
        root.addView(typeRow);

        photographerIdInput = new EditText(this);
        photographerIdInput.setSingleLine(true);
        photographerIdInput.setHint("Signed-in photographer UID");
        photographerIdInput.setEnabled(false);
        root.addView(photographerIdInput, matchWrap());
        ownershipNote = text("This offering is attached to your signed-in Firebase account.", 12, false);
        ownershipNote.setPadding(0, 0, 0, dp(8));
        root.addView(ownershipNote);

        loadButton = button("Load my offerings");
        root.addView(loadButton, matchWrap());
        loadButton.setOnClickListener(view -> refreshScreen());

        addButton = button("Add offering");
        root.addView(addButton, matchWrap());
        addButton.setOnClickListener(view -> openEditor(null));

        importCatalogButton = button("Load Barred rate card (21 packages + 19 add-ons)");
        root.addView(importCatalogButton, matchWrap());
        importCatalogButton.setOnClickListener(view -> confirmCatalogImport());

        statusText = text("Loading offerings…", 14, false);
        statusText.setPadding(0, dp(12), 0, dp(8));
        root.addView(statusText);

        offeringList = new LinearLayout(this);
        offeringList.setOrientation(LinearLayout.VERTICAL);
        root.addView(offeringList, matchWrap());
    }

    private void refreshScreen() {
        if (browseButton == null) return;
        styleToggleButton(browseButton, !managementMode);
        styleToggleButton(manageButton, managementMode);
        manageButton.setVisibility(photographerAccount ? View.VISIBLE : View.GONE);
        for (int i = 0; i < typeButtons.size(); i++) {
            styleToggleButton(typeButtons.get(i), i == selectedType);
        }
        photographerIdInput.setVisibility(managementMode ? View.VISIBLE : View.GONE);
        ownershipNote.setVisibility(managementMode ? View.VISIBLE : View.GONE);
        loadButton.setVisibility(managementMode ? View.VISIBLE : View.GONE);
        addButton.setVisibility(managementMode ? View.VISIBLE : View.GONE);
        importCatalogButton.setVisibility(managementMode ? View.VISIBLE : View.GONE);
        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
        offeringList.removeAllViews();
        if (!profileLoaded) {
            statusText.setText("Checking account access…");
            return;
        }
        String photographerId = currentUid == null ? "" : currentUid;
        if (managementMode && photographerId.isEmpty()) {
            statusText.setText("Could not find your signed-in Firebase UID. Log out and back in.");
            return;
        }
        BaseOfferingRepository<?> repository = repositoryForType();
        activeListener = managementMode
                ? repository.listenPhotographer(photographerId, (snapshot, error) -> showSnapshot(repository, snapshot, error))
                : repository.listenActive((snapshot, error) -> showSnapshot(repository, snapshot, error));
        statusText.setText(managementMode ? "Your offerings" : "Available to browse");
    }

    private void showSnapshot(BaseOfferingRepository<?> repository, QuerySnapshot snapshot,
                              com.google.firebase.firestore.FirebaseFirestoreException error) {
        runOnUiThread(() -> {
            offeringList.removeAllViews();
            if (error != null) {
                statusText.setText("Could not load offerings: " + error.getLocalizedMessage());
                return;
            }
            if (snapshot == null || snapshot.isEmpty()) {
                statusText.setText("No " + typeName().toLowerCase(Locale.ROOT) + " yet.");
                return;
            }
            statusText.setText(managementMode ? "Your " + typeName().toLowerCase(Locale.ROOT) : "Available " + typeName().toLowerCase(Locale.ROOT));
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                Object offering = repository.decode(document);
                if (offering != null) offeringList.addView(offeringCard(offering));
            }
        });
    }

    private View offeringCard(Object offering) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(getColor(R.color.lenslink_card));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.lenslink_border));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        String name = nameOf(offering);
        String category = categoryOf(offering);
        if (!category.isEmpty()) content.addView(text(category.toUpperCase(Locale.ROOT), 12, true));
        content.addView(text(name, 19, true));
        content.addView(text(String.format(Locale.getDefault(), "Price: R %.2f", priceOf(offering)), 16, true));
        if (offering instanceof PackageOffering && ((PackageOffering) offering).getTwoDayPrice() > 0) {
            content.addView(text(String.format(Locale.getDefault(), "Two-day price: R %.2f",
                    ((PackageOffering) offering).getTwoDayPrice()), 14, true));
        }
        if (offering instanceof PackageOffering && ((PackageOffering) offering).getPartnerPrice() > 0) {
            content.addView(text(String.format(Locale.getDefault(), "Partner price: R %.2f",
                    ((PackageOffering) offering).getPartnerPrice()), 14, true));
        }
        String description = descriptionOf(offering);
        if (!description.isEmpty()) content.addView(text(description, 14, false));
        List<String> inclusions = inclusionsOf(offering);
        if (!inclusions.isEmpty()) content.addView(text("Includes: " + String.join(", ", inclusions), 14, false));
        List<String> addonIds = addonIdsOf(offering);
        if (!addonIds.isEmpty()) {
            content.addView(text(managementMode
                    ? "Add-on IDs: " + String.join(", ", addonIds)
                    : "Optional add-ons are listed in the Add-ons tab.", 13, false));
        }
        if (offering instanceof ServiceOffering) {
            long duration = ((ServiceOffering) offering).getDurationMinutes();
            if (duration > 0) content.addView(text("Duration: " + duration + " minutes", 14, false));
        }
        if (offering instanceof PackageOffering) {
            List<String> serviceIds = ((PackageOffering) offering).getServiceIds();
            if (!serviceIds.isEmpty()) content.addView(text("Service IDs: " + String.join(", ", serviceIds), 13, false));
        }
        if (managementMode) {
            LinearLayout actions = row();
            MaterialButton edit = button("Edit");
            MaterialButton delete = button("Delete");
            actions.addView(edit, weightedParams());
            actions.addView(delete, weightedParams());
            content.addView(actions);
            edit.setOnClickListener(view -> openEditor(offering));
            delete.setOnClickListener(view -> confirmDelete(offering));
        }
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        return card;
    }

    private void openEditor(Object existing) {
        if (!photographerAccount || !managementMode) {
            toast("Only photographers can manage offerings.");
            return;
        }
        String ownerId = currentUid == null ? "" : currentUid;
        if (ownerId.isEmpty()) {
            toast("Could not find your signed-in Firebase UID.");
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(4), dp(8), dp(4));
        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form);

        EditText name = formInput(form, "Name", false);
        EditText description = formInput(form, "Description", false);
        EditText category = selectedType == SERVICES ? null : formInput(form, "Category", false);
        EditText price = formInput(form, "Price (number)", true);
        EditText twoDayPrice = selectedType == PACKAGES ? formInput(form, "Optional two-day price", true) : null;
        EditText partnerPrice = selectedType == PACKAGES ? formInput(form, "Optional partner price", true) : null;
        EditText inclusions = null;
        EditText addonIds = null;
        EditText duration = null;
        EditText serviceIds = null;
        if (selectedType == SERVICES) {
            duration = formInput(form, "Duration in minutes", true);
            inclusions = formInput(form, "Inclusions (comma separated)", false);
            addonIds = formInput(form, "Add-on document IDs (comma separated)", false);
        } else if (selectedType == PACKAGES) {
            serviceIds = formInput(form, "Included service document IDs (comma separated)", false);
            inclusions = formInput(form, "Inclusions (comma separated)", false);
            addonIds = formInput(form, "Add-on document IDs (comma separated)", false);
        }
        CheckBox active = new CheckBox(this);
        active.setText("Available for clients to browse");
        active.setChecked(existing == null || activeOf(existing));
        form.addView(active);

        name.setText(existing == null ? "" : nameOf(existing));
        description.setText(existing == null ? "" : descriptionOf(existing));
        if (category != null) category.setText(existing == null ? "" : categoryOf(existing));
        price.setText(existing == null ? "" : String.valueOf(priceOf(existing)));
        if (twoDayPrice != null && existing instanceof PackageOffering
                && ((PackageOffering) existing).getTwoDayPrice() > 0) {
            twoDayPrice.setText(String.valueOf(((PackageOffering) existing).getTwoDayPrice()));
        }
        if (partnerPrice != null && existing instanceof PackageOffering
                && ((PackageOffering) existing).getPartnerPrice() > 0) {
            partnerPrice.setText(String.valueOf(((PackageOffering) existing).getPartnerPrice()));
        }
        if (inclusions != null) inclusions.setText(existing == null ? "" : String.join(", ", inclusionsOf(existing)));
        if (addonIds != null) addonIds.setText(existing == null ? "" : String.join(", ", addonIdsOf(existing)));
        if (duration != null && existing instanceof ServiceOffering) {
            duration.setText(String.valueOf(((ServiceOffering) existing).getDurationMinutes()));
        }
        if (serviceIds != null && existing instanceof PackageOffering) {
            serviceIds.setText(String.join(", ", ((PackageOffering) existing).getServiceIds()));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add " + singularType() : "Edit " + singularType())
                .setView(formScroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        EditText finalDuration = duration;
        EditText finalServiceIds = serviceIds;
        EditText finalCategory = category;
        EditText finalTwoDayPrice = twoDayPrice;
        EditText finalPartnerPrice = partnerPrice;
        EditText finalInclusions = inclusions;
        EditText finalAddonIds = addonIds;
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                String title = name.getText().toString().trim();
                double numericPrice = Double.parseDouble(price.getText().toString().trim());
                if (title.isEmpty() || numericPrice < 0) throw new IllegalArgumentException();
                double secondDayPrice = finalTwoDayPrice == null || finalTwoDayPrice.getText().toString().trim().isEmpty()
                        ? 0 : Double.parseDouble(finalTwoDayPrice.getText().toString().trim());
                double pairedPrice = finalPartnerPrice == null || finalPartnerPrice.getText().toString().trim().isEmpty()
                        ? 0 : Double.parseDouble(finalPartnerPrice.getText().toString().trim());
                if (secondDayPrice < 0 || pairedPrice < 0) throw new IllegalArgumentException();
                long minutes = finalDuration == null || finalDuration.getText().toString().trim().isEmpty()
                        ? 0 : Long.parseLong(finalDuration.getText().toString().trim());
                if (minutes < 0) throw new IllegalArgumentException();
                Object value = makeOffering(existing, ownerId, title,
                        description.getText().toString().trim(), numericPrice, minutes,
                        finalCategory == null ? "" : finalCategory.getText().toString().trim(), secondDayPrice, pairedPrice,
                        finalServiceIds == null ? Collections.emptyList() : split(finalServiceIds.getText().toString()),
                        finalInclusions == null ? Collections.emptyList() : split(finalInclusions.getText().toString()),
                        finalAddonIds == null ? Collections.emptyList() : split(finalAddonIds.getText().toString()), active.isChecked());
                saveOffering(existing, value, dialog);
            } catch (IllegalArgumentException exception) {
                toast("Enter a name and a valid, non-negative price and duration.");
            }
        }));
        dialog.show();
    }

    private void saveOffering(Object existing, Object value, AlertDialog dialog) {
        BaseOfferingRepository repository = repositoryForType();
        com.google.android.gms.tasks.Task<?> task = existing == null
                ? repository.create(value)
                : repository.update(idOf(existing), value);
        task.addOnSuccessListener(result -> {
            toast("Offering saved.");
            dialog.dismiss();
            refreshScreen();
        }).addOnFailureListener(error -> toast("Save failed: " + error.getLocalizedMessage()));
    }

    private void confirmDelete(Object offering) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + nameOf(offering) + "?")
                .setMessage("This removes the offering from the catalogue. Existing bookings are not changed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> repositoryForType().delete(idOf(offering))
                        .addOnSuccessListener(unused -> toast("Offering deleted."))
                        .addOnFailureListener(error -> toast("Delete failed: " + error.getLocalizedMessage())))
                .show();
    }

    private void confirmCatalogImport() {
        if (!photographerAccount || !managementMode) {
            toast("Only photographers can load a rate card.");
            return;
        }
        String photographerUid = currentUid == null ? "" : currentUid;
        if (photographerUid.isEmpty()) {
            toast("Could not find your signed-in Firebase UID.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Load the Barred rate card?")
                .setMessage("This creates or replaces 21 package documents and 19 add-on documents for this photographer. Document IDs are scoped to the entered UID. Continue only if these rates belong to this photographer.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Load catalog", (dialog, which) -> {
                    try {
                        new CatalogSeeder().importBarredCatalog(this, photographerUid)
                                .addOnSuccessListener(count -> {
                                    toast(count + " catalog records loaded.");
                                    refreshScreen();
                                })
                                .addOnFailureListener(error -> toast("Catalog import failed: " + error.getLocalizedMessage()));
                    } catch (Exception error) {
                        toast("Could not read the catalog: " + error.getLocalizedMessage());
                    }
                })
                .show();
    }

    private Object makeOffering(Object existing, String ownerId, String name, String description,
                                double price, long duration, String category, double twoDayPrice,
                                double partnerPrice,
                                List<String> serviceIds,
                                List<String> inclusions, List<String> addonIds, boolean active) {
        String id = existing == null ? null : idOf(existing);
        if (selectedType == SERVICES) {
            return new ServiceOffering(id, ownerId, name, description, price, duration,
                    inclusions, addonIds, active);
        }
        if (selectedType == PACKAGES) {
            return new PackageOffering(id, ownerId, category, name, description, price, twoDayPrice, partnerPrice, serviceIds,
                    inclusions, addonIds, active);
        }
        return new AddonOffering(id, ownerId, category, name, description, price, active);
    }

    private EditText formInput(LinearLayout form, String hint, boolean numeric) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(!hint.contains("comma separated"));
        if (numeric) input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(4);
        form.addView(input, params);
        return input;
    }

    private BaseOfferingRepository<?> repositoryForType() {
        if (selectedType == PACKAGES) return packageRepository;
        if (selectedType == ADDONS) return addonRepository;
        return serviceRepository;
    }

    private String typeName() {
        return selectedType == PACKAGES ? "Packages" : selectedType == ADDONS ? "Add-ons" : "Services";
    }

    private String singularType() {
        return selectedType == PACKAGES ? "package" : selectedType == ADDONS ? "add-on" : "service";
    }

    private String idOf(Object item) {
        if (item instanceof ServiceOffering) return ((ServiceOffering) item).getId();
        if (item instanceof PackageOffering) return ((PackageOffering) item).getId();
        return ((AddonOffering) item).getId();
    }

    private String nameOf(Object item) {
        if (item instanceof ServiceOffering) return ((ServiceOffering) item).getName();
        if (item instanceof PackageOffering) return ((PackageOffering) item).getName();
        return ((AddonOffering) item).getName();
    }

    private String descriptionOf(Object item) {
        if (item instanceof ServiceOffering) return safe(((ServiceOffering) item).getDescription());
        if (item instanceof PackageOffering) return safe(((PackageOffering) item).getDescription());
        return safe(((AddonOffering) item).getDescription());
    }

    private String categoryOf(Object item) {
        if (item instanceof PackageOffering) return safe(((PackageOffering) item).getCategory());
        if (item instanceof AddonOffering) return safe(((AddonOffering) item).getCategory());
        return "";
    }

    private double priceOf(Object item) {
        if (item instanceof ServiceOffering) return ((ServiceOffering) item).getPrice();
        if (item instanceof PackageOffering) return ((PackageOffering) item).getPrice();
        return ((AddonOffering) item).getPrice();
    }

    private boolean activeOf(Object item) {
        if (item instanceof ServiceOffering) return ((ServiceOffering) item).isActive();
        if (item instanceof PackageOffering) return ((PackageOffering) item).isActive();
        return ((AddonOffering) item).isActive();
    }

    private List<String> inclusionsOf(Object item) {
        if (item instanceof ServiceOffering) return safeList(((ServiceOffering) item).getInclusions());
        if (item instanceof PackageOffering) return safeList(((PackageOffering) item).getInclusions());
        return Collections.emptyList();
    }

    private List<String> addonIdsOf(Object item) {
        if (item instanceof ServiceOffering) return safeList(((ServiceOffering) item).getAddonIds());
        if (item instanceof PackageOffering) return safeList(((PackageOffering) item).getAddonIds());
        return Collections.emptyList();
    }

    private List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String safe(String value) { return value == null ? "" : value; }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.lenslink_text));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }

    private MaterialButton button(String label) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        return button;
    }

    private void styleToggleButton(MaterialButton button, boolean selected) {
        button.setEnabled(true);
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(
                selected ? R.color.lenslink_ink : R.color.lenslink_soft)));
        button.setTextColor(getColor(selected ? R.color.white : R.color.lenslink_text));
    }

    private LinearLayout.LayoutParams weightedParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(2), dp(4), dp(2), dp(4));
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (activeListener != null) activeListener.remove();
        super.onDestroy();
    }
}
