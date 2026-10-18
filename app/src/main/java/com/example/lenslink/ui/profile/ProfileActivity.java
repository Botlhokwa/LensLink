package com.example.lenslink.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.repository.RepositoryCallback;
import com.example.lenslink.util.UiUtils;
import com.example.lenslink.util.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import static com.example.lenslink.util.UiUtils.applyFieldError;
import static com.example.lenslink.util.UiUtils.clearErrorOnEdit;
import static com.example.lenslink.util.UiUtils.hideKeyboard;
import static com.example.lenslink.util.UiUtils.textOf;

public class ProfileActivity extends AppCompatActivity {

    private enum Mode { LOADING, LOAD_FAILED, VIEWING, EDITING, SAVING }

    private AuthRepository authRepository;

    private TextInputLayout tilFullName, tilEmail, tilPhone, tilRole;
    private TextInputEditText etFullName, etEmail, etPhone, etRole;
    private MaterialButton btnPrimary, btnCancel, btnLogout;
    private LinearProgressIndicator progressIndicator;
    private TextView tvFormError;

    private Mode mode = Mode.LOADING;

    @Nullable
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository();

        // Protected-screen guard: no signed-in user means back to Login.
        if (!authRepository.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }

        UiUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_profile);
        UiUtils.applySystemBarInsets(findViewById(R.id.profileRoot));

        bindViews();
        clearErrorOnEdit(etFullName, tilFullName);
        clearErrorOnEdit(etPhone, tilPhone);

        btnPrimary.setOnClickListener(v -> onPrimaryClicked());
        btnCancel.setOnClickListener(v -> cancelEditing());
        btnLogout.setOnClickListener(v -> logout());

        loadProfile();
    }

    private void bindViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilRole = findViewById(R.id.tilRole);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etRole = findViewById(R.id.etRole);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnCancel = findViewById(R.id.btnCancel);
        btnLogout = findViewById(R.id.btnLogout);
        tvFormError = findViewById(R.id.tvFormError);
    }

    // Loading
    private void loadProfile() {
        clearFormError();
        mode = Mode.LOADING;
        renderState();

        authRepository.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (isActivityGone()) return;
                currentUser = user;
                bindUser(user);
                mode = Mode.VIEWING;
                renderState();
            }

            @Override
            public void onProfileMissing() {
                if (isActivityGone()) return;
                mode = Mode.LOAD_FAILED;
                renderState();
                showFormError(getString(R.string.error_profile_missing));
            }

            @Override
            public void onError(String message) {
                if (isActivityGone()) return;
                mode = Mode.LOAD_FAILED;
                renderState();
                showFormError(message);
            }
        });
    }

    // Editing
    private void onPrimaryClicked() {
        switch (mode) {
            case VIEWING:
                clearFormError();
                mode = Mode.EDITING;
                renderState();
                etFullName.requestFocus();
                break;
            case EDITING:
                saveChanges();
                break;
            case LOAD_FAILED:
                loadProfile();
                break;
            default:
                break; // LOADING / SAVING: button is disabled anyway
        }
    }

    private void cancelEditing() {
        if (currentUser == null) return;
        bindUser(currentUser);            // discard unsaved edits
        tilFullName.setError(null);
        tilPhone.setError(null);
        clearFormError();
        hideKeyboard(this);
        mode = Mode.VIEWING;
        renderState();
    }

    private void saveChanges() {
        if (currentUser == null) return;

        String fullName = textOf(etFullName);
        String phone = textOf(etPhone);

        boolean valid = true;
        valid &= applyFieldError(tilFullName, ValidationUtils.validateFullName(fullName));
        valid &= applyFieldError(tilPhone, ValidationUtils.validatePhone(phone));
        if (!valid) return;

        String newPhone = phone.isEmpty() ? null : phone;

        // Nothing changed: skip the network call.
        if (fullName.equals(currentUser.getFullName())
                && Objects.equals(newPhone, currentUser.getPhone())) {
            hideKeyboard(this);
            mode = Mode.VIEWING;
            renderState();
            return;
        }

        clearFormError();
        hideKeyboard(this);
        mode = Mode.SAVING;
        renderState();

        authRepository.updateProfile(fullName, phone, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isActivityGone() || currentUser == null) return;
                currentUser.setFullName(fullName);
                currentUser.setPhone(newPhone);
                bindUser(currentUser);
                mode = Mode.VIEWING;
                renderState();
                Toast.makeText(ProfileActivity.this,
                        R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (isActivityGone()) return;
                // Keep the user's edits so they can try again.
                mode = Mode.EDITING;
                renderState();
                showFormError(message);
            }
        });
    }

    private void logout() {
        authRepository.logout();
        AppNavigator.goToLogin(this); // fresh back stack
    }

    public ProfileActivity() {
        super();
    }

    // UI helpers
    private void renderState() {
        boolean editing = mode == Mode.EDITING;
        boolean busy = mode == Mode.LOADING || mode == Mode.SAVING;

        // Only name and phone are ever editable.
        tilFullName.setEnabled(editing);
        tilPhone.setEnabled(editing);
        tilEmail.setEnabled(false);
        tilRole.setEnabled(false);

        btnCancel.setVisibility(editing || mode == Mode.SAVING ? View.VISIBLE : View.GONE);
        btnCancel.setEnabled(editing);
        btnLogout.setEnabled(mode != Mode.SAVING);

        switch (mode) {
            case LOADING:
                btnPrimary.setText(R.string.button_loading);
                break;
            case EDITING:
                btnPrimary.setText(R.string.button_save_changes);
                break;
            case SAVING:
                btnPrimary.setText(R.string.button_saving);
                break;
            case LOAD_FAILED:
                btnPrimary.setText(R.string.button_try_again);
                break;
            default: // LOADING, VIEWING
                btnPrimary.setText(R.string.button_edit_profile);
                break;
        }
        UiUtils.setButtonLoading(btnPrimary, busy);
    }

    private void bindUser(User user) {
        etFullName.setText(user.getFullName());
        etEmail.setText(user.getEmail());
        etPhone.setText(user.getPhone() == null ? "" : user.getPhone());
        etRole.setText(roleLabel(user.getRole()));
    }

    private String roleLabel(@Nullable String role) {
        if (Role.CLIENT.equals(role)) return getString(R.string.role_client);
        if (Role.PHOTOGRAPHER.equals(role)) return getString(R.string.role_photographer);
        return role == null ? "" : role;
    }

    private void showFormError(String message) {
        tvFormError.setText(message);
        tvFormError.setVisibility(View.VISIBLE);
    }

    private void clearFormError() {
        tvFormError.setText("");
        tvFormError.setVisibility(View.GONE);
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}