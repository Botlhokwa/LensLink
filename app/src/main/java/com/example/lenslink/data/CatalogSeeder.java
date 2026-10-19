package com.example.lenslink.data;

import android.content.Context;

import com.example.lenslink.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Imports the photographer rate-card sample after the user confirms in the UI. */
public class CatalogSeeder {
    private final FirebaseFirestore firestore;

    public CatalogSeeder() {
        this(FirebaseFirestore.getInstance());
    }

    CatalogSeeder(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<Integer> importBarredCatalog(Context context, String photographerUid) throws Exception {
        if (photographerUid == null || photographerUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Photographer UID is required.");
        }

        JSONObject catalog = new JSONObject(readSeedFile(context));
        JSONObject collections = catalog.getJSONObject("collections");
        WriteBatch batch = firestore.batch();
        int writes = addCollection(batch, collections.getJSONArray("packages"), "packages", photographerUid);
        writes += addCollection(batch, collections.getJSONArray("addons"), "addons", photographerUid);
        final int recordCount = writes;
        return batch.commit().continueWith(task -> {
            task.getResult();
            return recordCount;
        });
    }

    private int addCollection(WriteBatch batch, JSONArray records, String collection,
                              String photographerUid) throws Exception {
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            String documentId = photographerUid + "_" + record.getString("documentId");
            Map<String, Object> fields = toMap(record.getJSONObject("fields"));
            fields.put("photographerId", photographerUid);
            Object addonIdsValue = fields.get("addonIds");
            if (addonIdsValue instanceof List) {
                List<Object> scopedAddonIds = new ArrayList<>();
                for (Object addonId : (List<?>) addonIdsValue) {
                    scopedAddonIds.add(photographerUid + "_" + addonId);
                }
                fields.put("addonIds", scopedAddonIds);
            }
            DocumentReference reference = firestore.collection(collection).document(documentId);
            batch.set(reference, fields);
        }
        return records.length();
    }

    private String readSeedFile(Context context) throws Exception {
        try (InputStream input = context.getResources().openRawResource(R.raw.barred_catalog_seed);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private Map<String, Object> toMap(JSONObject object) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = object.get(key);
            if (value == JSONObject.NULL) {
                result.put(key, null);
            } else if (value instanceof JSONObject) {
                result.put(key, toMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                result.put(key, toList((JSONArray) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private List<Object> toList(JSONArray array) throws Exception {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value == JSONObject.NULL) {
                result.add(null);
            } else if (value instanceof JSONObject) {
                result.add(toMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                result.add(toList((JSONArray) value));
            } else {
                result.add(value);
            }
        }
        return result;
    }
}
