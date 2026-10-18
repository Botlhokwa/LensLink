package com.example.lenslink.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.util.UiUtils;
import com.example.lenslink.util.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import static com.example.lenslink.util.UiUtils.applyFieldError;
import static com.example.lenslink.util.UiUtils.clearErrorOnEdit;
import static com.example.lenslink.util.UiUtils.hideKeyboard;
import static com.example.lenslink.util.UiUtils.rawTextOf;
import static com.example.lenslink.util.UiUtils.textOf;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository authRepository;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressIndicator;
    private TextView tvFormError;

    private boolean isSubmitting = false;

    /** Optional message to show on arrival (e.g. "session expired"). */
    public static final String EXTRA_MESSAGE = "extra_message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_login);
        UiUtils.applySystemBarInsets(findViewById(R.id.loginRoot));

        authRepository = new AuthRepository();

        bindViews();
        setupListeners();
        findViewById(R.id.toggleLogin).setSelected(true);
        renderState();

        String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (message != null && !message.isEmpty()) {
            showFormError(message);
        }
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvFormError = findViewById(R.id.tvFormError);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        findViewById(R.id.toggleSignUp).setOnClickListener(v -> openRegister());
        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> openRegister());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> openForgotPassword());

        clearErrorOnEdit(etEmail, tilEmail);
        clearErrorOnEdit(etPassword, tilPassword);
    }

    // Login flow

    private void attemptLogin() {
        if (isSubmitting) return; // ignore repeated taps

        String email = textOf(etEmail);
        String password = rawTextOf(etPassword); // passwords are never trimmed

        boolean valid = true;
        valid &= applyFieldError(tilEmail, ValidationUtils.validateEmail(email));
        valid &= applyFieldError(tilPassword, ValidationUtils.validateLoginPassword(password));
        if (!valid) return;

        clearFormError();
        hideKeyboard(this);
        isSubmitting = true;
        renderState();

        authRepository.login(email, password, new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (isActivityGone()) return;
                isSubmitting = false;
                renderState();

                // An unrecognised role must not get into the app.
                if (!AppNavigator.goToHomeForRole(LoginActivity.this, user.getRole())) {
                    authRepository.logout();
                    showFormError(getString(R.string.error_profile_missing));
                }
            }

            @Override
            public void onProfileMissing() {
                if (isActivityGone()) return;
                isSubmitting = false;
                renderState();
                // Signed in but no profile: let the user finish setting it up.
                AppNavigator.goToCompleteProfile(LoginActivity.this);
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

    // UI helpers

    private void renderState() {
        boolean editable = !isSubmitting;
        tilEmail.setEnabled(editable);
        tilPassword.setEnabled(editable);

        btnLogin.setText(isSubmitting ? R.string.button_logging_in : R.string.button_log_in);
        UiUtils.setButtonLoading(btnLogin, isSubmitting);
    }

    private void showFormError(String message) {
        tvFormError.setText(message);
        tvFormError.setVisibility(View.VISIBLE);
    }

    private void clearFormError() {
        tvFormError.setText("");
        tvFormError.setVisibility(View.GONE);
    }

    private void openRegister() {
        if (isSubmitting) return;
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    private void openForgotPassword() {
        if (isSubmitting) return;
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        // Carry over whatever the user already typed.
        intent.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, textOf(etEmail));
        startActivity(intent);
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}