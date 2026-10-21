package com.example.lenslink.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.repository.RepositoryCallback;
import com.example.lenslink.util.UiUtils;
import com.example.lenslink.util.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import static com.example.lenslink.util.UiUtils.applyFieldError;
import static com.example.lenslink.util.UiUtils.clearErrorOnEdit;
import static com.example.lenslink.util.UiUtils.hideKeyboard;
import static com.example.lenslink.util.UiUtils.textOf;

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private AuthRepository authRepository;

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendReset;
    private LinearProgressIndicator progressIndicator;
    private TextView tvFormError, tvStatus;

    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_forgot_password);
        UiUtils.applySystemBarInsets(findViewById(R.id.forgotRoot));

        authRepository = new AuthRepository();

        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvFormError = findViewById(R.id.tvFormError);
        tvStatus = findViewById(R.id.tvStatus);

        String prefill = getIntent().getStringExtra(EXTRA_EMAIL);
        if (prefill != null && !prefill.isEmpty()) {
            etEmail.setText(prefill);
        }

        btnSendReset.setOnClickListener(v -> attemptReset());
        etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptReset();
                return true;
            }
            return false;
        });
        clearErrorOnEdit(etEmail, tilEmail);

        // Back to Login: Login is still underneath in the back stack.
        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> finish());

        renderState();
    }

    private void attemptReset() {
        if (isSubmitting) return; // ignore repeated taps

        String email = textOf(etEmail);
        if (!applyFieldError(tilEmail, ValidationUtils.validateEmail(email))) return;

        clearMessages();
        hideKeyboard(this);
        isSubmitting = true;
        renderState();

        authRepository.sendPasswordReset(email, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isActivityGone()) return;
                isSubmitting = false;
                renderState();
                // Only now, after Firebase confirmed, do we say anything was sent.
                tvStatus.setText(R.string.msg_reset_link_sent);
                tvStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (isActivityGone()) return;
                isSubmitting = false;
                renderState();
                tvFormError.setText(message);
                tvFormError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderState() {
        tilEmail.setEnabled(!isSubmitting);
        btnSendReset.setText(isSubmitting
                ? R.string.button_sending : R.string.button_send_reset_link);
        UiUtils.setButtonLoading(btnSendReset, isSubmitting);
    }

    private void clearMessages() {
        tvFormError.setText("");
        tvFormError.setVisibility(View.GONE);
        tvStatus.setText("");
        tvStatus.setVisibility(View.GONE);
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}