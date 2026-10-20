package com.example.lenslink.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.MainActivity;
import com.example.lenslink.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progress;
    private MaterialButton btnLogin, btnSignUp, btnGuest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // If user already logged in, launch MainActivity
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progressIndicator);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnLogout);
        btnGuest = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> performLogin());
        btnSignUp.setOnClickListener(v -> performSignUp());
        btnGuest.setOnClickListener(v -> performGuestLogin());
    }

    private void performLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            return;
        }

        setBusy(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setBusy(false);
                    toast("Welcome back!");
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("Sign in failed: " + e.getLocalizedMessage());
                });
    }

    private void performSignUp() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        setBusy(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setBusy(false);
                    toast("Account created successfully!");
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("Sign up failed: " + e.getLocalizedMessage());
                });
    }

    private void performGuestLogin() {
        setBusy(true);
        mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    setBusy(false);
                    toast("Signed in as Guest");
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("Guest sign-in failed: " + e.getLocalizedMessage());
                });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!busy);
        btnSignUp.setEnabled(!busy);
        btnGuest.setEnabled(!busy);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
