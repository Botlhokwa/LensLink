package com.example.lenslink.ui.temp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.R;
import com.example.lenslink.model.User;
import com.example.lenslink.navigation.AppNavigator;
import com.example.lenslink.repository.AuthRepository;
import com.example.lenslink.util.UiUtils;

public class TempHomeActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private TextView tvSignedInAs;

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
        setContentView(R.layout.activity_temp_home);
        UiUtils.applySystemBarInsets(findViewById(R.id.tempHomeRoot));

        tvSignedInAs = findViewById(R.id.tvSignedInAs);
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        findViewById(R.id.btnProfile).setOnClickListener(v -> AppNavigator.goToProfile(this));

        loadProfile();
    }

    private void loadProfile() {
        authRepository.fetchCurrentProfile(new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                tvSignedInAs.setText(getString(R.string.temp_home_signed_in_as,
                        user.getFullName(), user.getRole()));
            }

            @Override
            public void onProfileMissing() {
                if (isFinishing() || isDestroyed()) return;
                tvSignedInAs.setText(R.string.error_profile_missing);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                tvSignedInAs.setText(message);
            }
        });
    }

    private void logout() {
        authRepository.logout();
        // Fresh back stack: Back cannot return to this protected screen.
        AppNavigator.goToLogin(this);
    }
}