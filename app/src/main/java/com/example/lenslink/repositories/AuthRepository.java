package com.example.lenslink.repositories;
import com.example.lenslink.models.User;
import com.example.lenslink.utils.FirestoreConstants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void register(String fullName, String email, String password, String role, RepositoryCallback<User> cb) {
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(res -> {
            FirebaseUser fbu = res.getUser();
            if (fbu != null) {
                User u = new User(fbu.getUid(), fullName, email, "", role);
                db.collection(FirestoreConstants.USERS).document(fbu.getUid()).set(u)
                    .addOnSuccessListener(v -> cb.onSuccess(u))
                    .addOnFailureListener(e -> cb.onSuccess(u));
            } else {
                cb.onError(new Exception("Could not create the account. Please try again."));
            }
        }).addOnFailureListener(cb::onError);
    }

    public void login(String email, String password, String fallbackRole, RepositoryCallback<User> cb) {
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(res -> {
            FirebaseUser fbu = res.getUser();
            if (fbu != null) {
                db.collection(FirestoreConstants.USERS).document(fbu.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            cb.onSuccess(u);
                        } else {
                            cb.onSuccess(new User(fbu.getUid(), fallbackName(fallbackRole), email, "", fallbackRole));
                        }
                    })
                    .addOnFailureListener(e -> {
                        cb.onSuccess(new User(fbu.getUid(), fallbackName(fallbackRole), email, "", fallbackRole));
                    });
            } else {
                cb.onError(new Exception("Could not sign in. Please try again."));
            }
        }).addOnFailureListener(cb::onError);
    }

    private static String fallbackName(String role) {
        return FirestoreConstants.ROLE_PHOTOGRAPHER.equals(role) ? "Barred Production Studios" : "Bee Lemme";
    }

    public void logout() { auth.signOut(); }
    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }
}
