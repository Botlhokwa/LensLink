package com.example.lenslink.data;

import com.example.lenslink.model.PackageOffering;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PackageRepository extends BaseOfferingRepository<PackageOffering> {
    public PackageRepository() { super(); }
    public PackageRepository(FirebaseFirestore firestore) { super(firestore); }

    @Override protected String collectionName() { return "packages"; }

    @Override protected Map<String, Object> toFirestoreMap(PackageOffering item) {
        Map<String, Object> values = new HashMap<>();
        values.put("photographerId", item.getPhotographerId());
        values.put("category", item.getCategory());
        values.put("name", item.getName());
        values.put("description", item.getDescription());
        values.put("price", item.getPrice());
        values.put("twoDayPrice", item.getTwoDayPrice());
        values.put("partnerPrice", item.getPartnerPrice());
        values.put("serviceIds", item.getServiceIds());
        values.put("inclusions", item.getInclusions());
        values.put("addonIds", item.getAddonIds());
        values.put("active", item.isActive());
        return values;
    }

    @Override protected PackageOffering fromDocument(DocumentSnapshot document) {
        PackageOffering item = document.toObject(PackageOffering.class);
        if (item != null) item.setId(document.getId());
        return item;
    }
}
