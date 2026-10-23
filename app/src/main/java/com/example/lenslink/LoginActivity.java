package com.example.lenslink;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.lenslink.models.User;
import com.example.lenslink.repositories.AuthRepository;
import com.example.lenslink.repositories.RepositoryCallback;
import com.example.lenslink.utils.FirestoreConstants;
import com.example.lenslink.utils.Permissions;
import com.example.lenslink.utils.RoleGuard;
import com.example.lenslink.utils.SessionManager;
import com.example.lenslink.utils.SystemBars;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {
    private boolean isLogin = true;
    private AuthRepository authRepo;
    private SessionManager session;
    private EditText etFullName, etEmail, etPassword;
    private RadioButton rbPhotographer;
    private RadioGroup rgRole;
    private TextView tabLogin, tabSignUp;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        authRepo = new AuthRepository();

        // Already signed in: skip straight to the right home for the stored role.
        if (session.isLoggedIn() && authRepo.getCurrentUser() != null) {
            RoleGuard.routeByRole(this, session.getRole());
            return;
        }
        session.clear();

        setContentView(R.layout.activity_login);
        SystemBars.apply(this, findViewById(R.id.root), null);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        rbPhotographer = findViewById(R.id.rbPhotographer);
        tabLogin = findViewById(R.id.tabLogin);
        tabSignUp = findViewById(R.id.tabSignUp);
        btnSubmit = findViewById(R.id.btnSubmitAuth);

        tabLogin.setOnClickListener(v -> setAuthMode(true));
        tabSignUp.setOnClickListener(v -> setAuthMode(false));
        btnSubmit.setOnClickListener(v -> handleAuth());
        rgRole.setOnCheckedChangeListener((group, id) -> updateNameHint());
        updateNameHint();
    }

    private void updateNameHint() {
        etFullName.setHint(rbPhotographer.isChecked() ? "Studio Name" : "Full Name");
    }

    private void setAuthMode(boolean login) {
        this.isLogin = login;
        etFullName.setVisibility(login ? View.GONE : View.VISIBLE);
        btnSubmit.setText(login ? "Log In" : "Sign Up");
        tabLogin.setBackgroundResource(login ? R.drawable.bg_pill_dark : 0);
        tabLogin.setTextColor(ContextCompat.getColor(this, login ? R.color.surface_white : R.color.text_secondary));
        tabSignUp.setBackgroundResource(!login ? R.drawable.bg_pill_dark : 0);
        tabSignUp.setTextColor(ContextCompat.getColor(this, !login ? R.color.surface_white : R.color.text_secondary));
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Please wait…" : (isLogin ? "Log In" : "Sign Up"));
    }

    private void handleAuth() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        final String chosenRole = rbPhotographer.isChecked()
            ? FirestoreConstants.ROLE_PHOTOGRAPHER : FirestoreConstants.ROLE_CLIENT;

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isLogin && pass.length() < 6) {
            Toast.makeText(this, "Choose a password with at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        if (isLogin) {
            authRepo.login(email, pass, chosenRole, new RepositoryCallback<User>() {
                @Override public void onSuccess(User u) {
                    // RBAC: the account type picked on screen must match the role stored on the account.
                    if (!chosenRole.equalsIgnoreCase(u.getRole())) {
                        authRepo.logout();
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                            "This account is registered as " + Permissions.label(u.getRole())
                                + ". Switch the account type above and try again.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    session.save(u);
                    RoleGuard.routeByRole(LoginActivity.this, u.getRole());
                }
                @Override public void onError(Exception e) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            String name = etFullName.getText().toString().trim();
            if (name.isEmpty()) name = rbPhotographer.isChecked() ? "Barred Production Studios" : "Bee Lemme";
            authRepo.register(name, email, pass, chosenRole, new RepositoryCallback<User>() {
                @Override public void onSuccess(User u) {
                    session.save(u);
                    RoleGuard.routeByRole(LoginActivity.this, u.getRole());
                }
                @Override public void onError(Exception e) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
