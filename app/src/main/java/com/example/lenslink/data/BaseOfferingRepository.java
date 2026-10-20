package com.example.lenslink.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

/** Shared Firestore CRUD and live-query behavior for photographer offerings. */
public abstract class BaseOfferingRepository<T> {
    private final FirebaseFirestore firestore;

    protected BaseOfferingRepository() {
        this(FirebaseFirestore.getInstance());
    }

    protected BaseOfferingRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    protected abstract String collectionName();
    protected abstract Map<String, Object> toFirestoreMap(T item);
    protected abstract T fromDocument(DocumentSnapshot document);

    protected FirebaseFirestore firestore() { return firestore; }

    public Task<String> create(T item) {
        return firestore.collection(collectionName()).add(toFirestoreMap(item))
                .continueWith(task -> task.getResult().getId());
    }

    /** Replaces the editable offering fields while preserving its Firestore document ID. */
    public Task<Void> update(String documentId, T item) {
        return firestore.collection(collectionName()).document(documentId).set(toFirestoreMap(item));
    }

    public Task<Void> delete(String documentId) {
        return firestore.collection(collectionName()).document(documentId).delete();
    }

    /** Live list for client browsing. Inactive offerings are excluded. */
    public ListenerRegistration listenActive(EventListener<QuerySnapshot> listener) {
        return firestore.collection(collectionName()).whereEqualTo("active", true)
                .addSnapshotListener(listener);
    }

    /** Live list for a photographer's management screen. */
    public ListenerRegistration listenPhotographer(String photographerId,
                                                    EventListener<QuerySnapshot> listener) {
        return firestore.collection(collectionName()).whereEqualTo("photographerId", photographerId)
                .addSnapshotListener(listener);
    }

    public T decode(DocumentSnapshot document) {
        T item = fromDocument(document);
        return item;
    }
}
