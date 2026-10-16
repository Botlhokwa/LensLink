package com.example.lenslink;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.util.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class MainActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private LinearProgressIndicator progressIndicator;
    private TextView tvMessage;
    private MaterialButton btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_main);
        UiUtils.applySystemBarInsets(findViewById(R.id.splashRoot));

        authRepository = new AuthRepository();
        progressIndicator = findViewById(R.id.progressIndicator);
        tvMessage = findViewById(R.id.tvMessage);
        btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> checkSession());

        checkSession();
    }

    private void checkSession() {
        // No saved session: straight to Login.
        if (!authRepository.isUserLoggedIn()) {
            AppNavigator.goToLogin(this);
            return;
        }

        showChecking();

        authRepository.restoreSession(new AuthRepository.SessionCallback() {
            @Override
            public void onValid(User user) {
                if (isActivityGone()) return;
                // An unrecognised role must not get into the app.
                if (!AppNavigator.goToHomeForRole(MainActivity.this, user.getRole())) {
                    authRepository.logout();
                    AppNavigator.goToLogin(MainActivity.this,
                            getString(R.string.error_profile_missing));
                }
            }

            @Override
            public void onProfileMissing() {
                if (isActivityGone()) return;
                AppNavigator.goToCompleteProfile(MainActivity.this);
            }

            @Override
            public void onSessionInvalid() {
                if (isActivityGone()) return;
                AppNavigator.goToLogin(MainActivity.this,
                        getString(R.string.msg_session_expired));
            }

            @Override
            public void onError(String message) {
                if (isActivityGone()) return;
                showError(message); // stays signed in; user can retry
            }
        });
    }

    private void showChecking() {
        progressIndicator.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressIndicator.setVisibility(View.INVISIBLE);
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}