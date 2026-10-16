package com.example.lenslink.ui.auth;

import static com.example.lenslink.util.UiUtils.applyFieldError;
import static com.example.lenslink.util.UiUtils.clearErrorOnEdit;
import static com.example.lenslink.util.UiUtils.hideKeyboard;
import static com.example.lenslink.util.UiUtils.rawTextOf;
import static com.example.lenslink.util.UiUtils.textOf;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.repository.RepositoryCallback;
import com.example.lenslink.util.UiUtils;
import com.example.lenslink.util.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.lenslink.navigation.AppNavigator;

public class RegisterActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialButtonToggleGroup roleToggleGroup;
    private MaterialButton btnRoleClient, btnRolePhotographer, btnCreateAccount;
    private LinearProgressIndicator progressIndicator;
    private TextView tvFormError;

    private boolean isSubmitting = false;

    @Nullable
    private User pendingProfile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_register);
        UiUtils.applySystemBarInsets(findViewById(R.id.registerRoot));

        authRepository = new AuthRepository();

        bindViews();
        setupToggle();
        setupListeners();
        roleToggleGroup.check(R.id.btnRoleClient); // sensible default
        renderState();
    }

    private void bindViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        roleToggleGroup = findViewById(R.id.roleToggleGroup);
        btnRoleClient = findViewById(R.id.btnRoleClient);
        btnRolePhotographer = findViewById(R.id.btnRolePhotographer);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressIndicator = findViewById(R.id.progressIndicator);
        tvFormError = findViewById(R.id.tvFormError);
    }

    private void setupToggle() {
        findViewById(R.id.toggleSignUp).setSelected(true);
        findViewById(R.id.toggleLogin).setOnClickListener(v -> openLogin());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> openLogin());
    }

    private void setupListeners() {
        btnCreateAccount.setOnClickListener(v -> attemptRegister());

        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });

        // Clear a field's error as soon as the user edits it.
        clearErrorOnEdit(etFullName, tilFullName);
        clearErrorOnEdit(etEmail, tilEmail);
        clearErrorOnEdit(etPhone, tilPhone);
        clearErrorOnEdit(etPassword, tilPassword);
        clearErrorOnEdit(etConfirmPassword, tilConfirmPassword);
    }

    // Registration flow

    private void attemptRegister() {
        if (isSubmitting) return; // ignore repeated taps

        // Account exists but profile is missing: retry only the profile write.
        if (pendingProfile != null) {
            retryProfileSave();
            return;
        }

        String fullName = textOf(etFullName);
        String email = textOf(etEmail);
        String phone = textOf(etPhone);
        String password = rawTextOf(etPassword);       // passwords are never trimmed
        String confirmPassword = rawTextOf(etConfirmPassword);
        String role = roleToggleGroup.getCheckedButtonId() == R.id.btnRolePhotographer
                ? Role.PHOTOGRAPHER : Role.CLIENT;

        // '&=' (not '&&') so every invalid field shows its error at once.
        boolean valid = true;
        valid &= applyFieldError(tilFullName, ValidationUtils.validateFullName(fullName));
        valid &= applyFieldError(tilEmail, ValidationUtils.validateEmail(email));
        valid &= applyFieldError(tilPhone, ValidationUtils.validatePhone(phone));
        valid &= applyFieldError(tilPassword, ValidationUtils.validatePassword(password));
        valid &= applyFieldError(tilConfirmPassword,
                ValidationUtils.validateConfirmPassword(password, confirmPassword));
        if (!valid) return;

        clearFormError();
        hideKeyboard(this);
        isSubmitting = true;
        renderState();

        authRepository.register(fullName, email, phone, password, role,
                new AuthRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (isActivityGone()) return;
                        onRegistrationComplete(user);
                    }

                    @Override
                    public void onProfileSaveFailed(User profile, String message) {
                        if (isActivityGone()) return;
                        isSubmitting = false;
                        pendingProfile = profile;
                        renderState();
                        showFormError(getString(R.string.error_profile_save_failed));
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

    private void retryProfileSave() {
        if (pendingProfile == null) return;

        clearFormError();
        isSubmitting = true;
        renderState();

        authRepository.saveNewProfile(pendingProfile, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                if (isActivityGone()) return;
                onRegistrationComplete(result);
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

    private void onRegistrationComplete(User user) {
        isSubmitting = false;
        pendingProfile = null;
        renderState();

        Toast.makeText(this, R.string.msg_account_created, Toast.LENGTH_LONG).show();
        if (!AppNavigator.goToHomeForRole(this, user.getRole())) {
            authRepository.logout();
            openLogin();
        }
    }

    // UI helpers

    private void renderState() {
        boolean formEditable = !isSubmitting && pendingProfile == null;

        tilFullName.setEnabled(formEditable);
        tilEmail.setEnabled(formEditable);
        tilPhone.setEnabled(formEditable);
        tilPassword.setEnabled(formEditable);
        tilConfirmPassword.setEnabled(formEditable);
        btnRoleClient.setEnabled(formEditable);
        btnRolePhotographer.setEnabled(formEditable);

        btnCreateAccount.setEnabled(!isSubmitting);
        progressIndicator.setVisibility(isSubmitting ? View.VISIBLE : View.INVISIBLE);

        if (isSubmitting) {
            btnCreateAccount.setText(R.string.button_creating_account);
        } else if (pendingProfile != null) {
            btnCreateAccount.setText(R.string.button_retry_profile);
        } else {
            btnCreateAccount.setText(R.string.button_create_account);
        }
    }


    private void showFormError(String message) {
        tvFormError.setText(message);
        tvFormError.setVisibility(View.VISIBLE);
    }

    private void clearFormError() {
        tvFormError.setText("");
        tvFormError.setVisibility(View.GONE);
    }

    private void openLogin() {
        if (isSubmitting) return;
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}