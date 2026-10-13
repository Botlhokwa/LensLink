package com.example.lenslink.database;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Fetching packages
    public void getPackages(OnCompleteListener<QuerySnapshot> listener) {
        db.collection("packages").get().addOnCompleteListener(listener);
    }

    // The Safe Batch Write for Bookings
    public void createBooking(Booking newBooking, List<DailySchedule> scheduleBlocks) {
        WriteBatch batch = db.batch();

        // 1. Prepare the main booking document
        DocumentReference bookingRef = db.collection("Booking").document();
        newBooking.setBookingId(bookingRef.getId());
        batch.set(bookingRef, newBooking);

        // 2. Prepare the daily schedule blocks safely using arrayUnion
        for (DailySchedule schedule : scheduleBlocks) {
            DocumentReference scheduleRef = db.collection("daily_schedules").document(schedule.getDate());

            // Map the data for a merge rather than an overwrite
            Map<String, Object> slotUpdates = new HashMap<>();
            slotUpdates.put("date", schedule.getDate());

            // Convert the List of slots to an array because arrayUnion requires varargs in Java
            Object[] slotsArray = schedule.getBookedSlots().toArray();
            slotUpdates.put("bookedSlots", FieldValue.arrayUnion(slotsArray));

            // SetOptions.merge() creates the document if it's a new day,
            // or safely appends the slots if the day already exists.
            batch.set(scheduleRef, slotUpdates, SetOptions.merge());
        }

        // 3. Commit the batch
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("FirebaseTest", "SUCCESS: Data written to Firestore!");
            } else {
                Log.e("FirebaseTest", "FAILED: " + task.getException().getMessage());
            }
        });
    }
}
