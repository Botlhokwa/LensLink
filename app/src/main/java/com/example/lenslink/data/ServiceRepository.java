package com.example.lenslink.data;

import com.example.lenslink.model.ServiceOffering;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ServiceRepository extends BaseOfferingRepository<ServiceOffering> {
    public ServiceRepository() { super(); }
    public ServiceRepository(FirebaseFirestore firestore) { super(firestore); }

    @Override protected String collectionName() { return "services"; }

    @Override protected Map<String, Object> toFirestoreMap(ServiceOffering item) {
        Map<String, Object> values = new HashMap<>();
        values.put("photographerId", item.getPhotographerId());
        values.put("name", item.getName());
        values.put("description", item.getDescription());
        values.put("price", item.getPrice());
        values.put("durationMinutes", item.getDurationMinutes());
        values.put("inclusions", item.getInclusions());
        values.put("addonIds", item.getAddonIds());
        values.put("active", item.isActive());
        return values;
    }

    @Override protected ServiceOffering fromDocument(DocumentSnapshot document) {
        ServiceOffering item = document.toObject(ServiceOffering.class);
        if (item != null) item.setId(document.getId());
        return item;
    }
}
