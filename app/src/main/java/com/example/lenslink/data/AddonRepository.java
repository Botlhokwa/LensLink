package com.example.lenslink.data;

import com.example.lenslink.model.AddonOffering;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddonRepository extends BaseOfferingRepository<AddonOffering> {
    public AddonRepository() { super(); }
    public AddonRepository(FirebaseFirestore firestore) { super(firestore); }

    @Override protected String collectionName() { return "addons"; }

    @Override protected Map<String, Object> toFirestoreMap(AddonOffering item) {
        Map<String, Object> values = new HashMap<>();
        values.put("photographerId", item.getPhotographerId());
        values.put("category", item.getCategory());
        values.put("name", item.getName());
        values.put("description", item.getDescription());
        values.put("price", item.getPrice());
        values.put("active", item.isActive());
        return values;
    }

    @Override protected AddonOffering fromDocument(DocumentSnapshot document) {
        AddonOffering item = document.toObject(AddonOffering.class);
        if (item != null) item.setId(document.getId());
        return item;
    }
}
