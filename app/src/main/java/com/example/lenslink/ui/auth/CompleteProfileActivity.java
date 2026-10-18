package com.example.lenslink.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import static com.example.lenslink.util.UiUtils.applyFieldError;
import static com.example.lenslink.util.UiUtils.clearErrorOnEdit;
import static com.example.lenslink.util.UiUtils.hideKeyboard;
import static com.example.lenslink.util.UiUtils.textOf;

public class CompleteProfileActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private TextInputLayout tilFullName, tilEmail, tilPhone;
    private TextInputEditText etFullName, etEmail, etPhone;
    private MaterialButtonToggleGroup roleToggleGroup;
    private MaterialButton btnRoleClient, btnRolePhotographer, btnComplete, btnLogout;
    private LinearProgressIndicator progressIndicator;
    private TextView tvFormError;

    private boolean isSubmitting = false;

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
        setContentView(R.layout.activity_complete_profile);
        UiUtils.applySystemBarInsets(findViewById(R.id.completeRoot));

        bindViews();

        etEmail.setText(authRepository.getCurrentUserEmail());
        roleToggleGroup.check(R.id.btnRoleClient);

        btnComplete.setOnClickListener(v -> attemptComplete());
        btnLogout.setOnClickListener(v -> logout());
        etPhone.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptComplete();
                return true;
            }
            return false;
        });
        clearErrorOnEdit(etFullName, tilFullName);
        clearErrorOnEdit(etPhone, tilPhone);

        renderState();
    }

    private void bindViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        roleToggleGroup = findViewById(R.id.roleToggleGroup);
        btnRoleClient = findViewById(R.id.btnRoleClient);
        btnRolePhotographer = findViewById(R.id.btnRolePhotographer);
        btnComplete = findViewById(R.id.btnComplete);
        btnLogout = findViewById(R.id.btnLogout);
        tvFormError = findViewById(R.id.tvFormError);
    }

    private void attemptComplete() {
        if (isSubmitting) return; // ignore repeated taps

        String fullName = textOf(etFullName);
        String phone = textOf(etPhone);
        String role = roleToggleGroup.getCheckedButtonId() == R.id.btnRolePhotographer
                ? Role.PHOTOGRAPHER : Role.CLIENT;

        boolean valid = true;
        valid &= applyFieldError(tilFullName, ValidationUtils.validateFullName(fullName));
        valid &= applyFieldError(tilPhone, ValidationUtils.validatePhone(phone));
        if (!valid) return;

        String uid = authRepository.getCurrentUserId();
        String email = authRepository.getCurrentUserEmail();
        if (uid == null || email == null) {
            AppNavigator.goToLogin(this, getString(R.string.msg_session_expired));
            return;
        }

        User profile = new User(uid, fullName, email,
                phone.isEmpty() ? null : phone, role, null);

        clearFormError();
        hideKeyboard(this);
        isSubmitting = true;
        renderState();

        authRepository.saveNewProfile(profile, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User saved) {
                if (isActivityGone()) return;
                AppNavigator.goToHomeForRole(CompleteProfileActivity.this, saved.getRole());
            }

            @Override
            public void onError(String message) {
                if (isActivityGone()) return;
                isSubmitting = false;
                renderState();
                showFormError(message);
            }
        });
    }

    private void logout() {
        if (isSubmitting) return;
        authRepository.logout();
        AppNavigator.goToLogin(this);
    }

    private void renderState() {
        boolean editable = !isSubmitting;
        tilFullName.setEnabled(editable);
        tilPhone.setEnabled(editable);
        tilEmail.setEnabled(false); // comes from the account; never editable here
        btnRoleClient.setEnabled(editable);
        btnRolePhotographer.setEnabled(editable);
        btnLogout.setEnabled(editable);
        btnComplete.setText(isSubmitting ? R.string.button_saving : R.string.button_complete_profile);
        UiUtils.setButtonLoading(btnComplete, isSubmitting);
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