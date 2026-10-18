package com.example.lenslink.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lenslink.model.Role;
import com.example.lenslink.model.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    public interface ProfileCallback {
        void onSuccess(User user);

        void onProfileMissing();

        void onError(String message);
    }

    public interface RegistrationCallback {
        void onSuccess(User user);

        void onProfileSaveFailed(User pendingProfile, String message);

        void onError(String message);
    }

    public interface SessionCallback {
        void onValid(User user);

        void onProfileMissing();

        void onSessionInvalid();

        void onError(String message);
    }

    private static final String USERS_COLLECTION = "users";
    private static final String FIELD_FULL_NAME = "fullName";
    private static final String FIELD_PHONE = "phone";

    private static final String MSG_GENERIC =
            "Something went wrong. Please try again.";
    private static final String MSG_SESSION_EXPIRED =
            "Your session has expired. Please log in again.";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Session
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void logout() {
        auth.signOut();
    }

    @Nullable
    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public void restoreSession(@NonNull SessionCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onSessionInvalid();
            return;
        }

        firebaseUser.reload()
                .addOnSuccessListener(unused ->
                        fetchProfile(firebaseUser.getUid(), new ProfileCallback() {
                            @Override
                            public void onSuccess(User user) {
                                callback.onValid(user);
                            }

                            @Override
                            public void onProfileMissing() {
                                callback.onProfileMissing();
                            }

                            @Override
                            public void onError(String message) {
                                callback.onError(message);
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        auth.signOut();
                        callback.onSessionInvalid();
                    } else {
                        callback.onError(describeAuthError(e));
                    }
                });
    }

    // Registration

    public void register(String fullName, String email, String phone,
                         String password, String role,
                         @NonNull RegistrationCallback callback) {

        // Never trust the UI: only CLIENT or PHOTOGRAPHER may self-register.
        if (!Role.isSelfRegistrationRole(role)) {
            callback.onError("Please choose a valid account type.");
            return;
        }

        String cleanEmail = email.trim();

        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(MSG_GENERIC);
                        return;
                    }

                    User profile = new User(
                            firebaseUser.getUid(),
                            fullName.trim(),
                            cleanEmail,
                            emptyToNull(phone),
                            role,
                            null);

                    saveNewProfile(profile, new RepositoryCallback<User>() {
                        @Override
                        public void onSuccess(User saved) {
                            callback.onSuccess(saved);
                        }

                        @Override
                        public void onError(String message) {
                            callback.onProfileSaveFailed(profile, message);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(describeAuthError(e)));
    }

    public void saveNewProfile(@NonNull User profile,
                               @NonNull RepositoryCallback<User> callback) {
        FirebaseUser current = auth.getCurrentUser();

        if (current == null || !current.getUid().equals(profile.getUserId())) {
            callback.onError(MSG_SESSION_EXPIRED);
            return;
        }
        if (!Role.isSelfRegistrationRole(profile.getRole())) {
            callback.onError("Please choose a valid account type.");
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(profile.getUserId())
                .set(profile)
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(e -> callback.onError(describeFirestoreError(e)));
    }

    // Login

    public void login(String email, String password,
                      @NonNull ProfileCallback callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError(MSG_GENERIC);
                        return;
                    }
                    fetchProfile(firebaseUser.getUid(), callback);
                })
                .addOnFailureListener(e -> callback.onError(describeAuthError(e)));
    }

    // Profile

    public void fetchCurrentProfile(@NonNull ProfileCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError(MSG_SESSION_EXPIRED);
            return;
        }
        fetchProfile(uid, callback);
    }

    private void fetchProfile(String uid, ProfileCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.exists() ? snapshot.toObject(User.class) : null;

                    // Missing document, or one without a usable role, counts as incomplete.
                    if (user == null || user.getRole() == null) {
                        callback.onProfileMissing();
                        return;
                    }
                    user.setUserId(uid);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(describeFirestoreError(e)));
    }

    public void updateProfile(String fullName, String phone,
                              @NonNull RepositoryCallback<Void> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError(MSG_SESSION_EXPIRED);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_FULL_NAME, fullName.trim());
        updates.put(FIELD_PHONE, emptyToNull(phone));

        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(describeFirestoreError(e)));
    }

    // Password reset

    public void sendPasswordReset(String email,
                                  @NonNull RepositoryCallback<Void> callback) {
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        // Unknown account: report success so existence isn't revealed.
                        callback.onSuccess(null);
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        callback.onError("Please enter a valid email address.");
                    } else {
                        callback.onError(describeAuthError(e));
                    }
                });
    }

    // Helpers

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String describeAuthError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "That password is too weak. Please choose a stronger one.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return "Incorrect email or password.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "No internet connection. Please check your network and try again.";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "Too many attempts. Please wait a moment and try again.";
        }
        return MSG_GENERIC;
    }

    private static String describeFirestoreError(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code =
                    ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "You don't have permission to do that.";
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return "Can't reach the server. Please check your connection and try again.";
            }
        }
        return MSG_GENERIC;
    }
}