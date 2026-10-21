package com.example.lenslink.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Writes a client booking and reserves its daily schedule slot atomically. */
public final class BookingRepository {
    private final FirebaseFirestore firestore;

    public BookingRepository() {
        this(FirebaseFirestore.getInstance());
    }

    BookingRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<String> createBooking(String clientId, String photographerId,
                                      String packageId, String packageName,
                                      List<String> serviceIds, List<String> serviceNames,
                                      List<String> addonIds, List<String> addonNames,
                                      String clientName, String date, String startTime, String endTime,
                                      boolean fullDay, String location, double totalPrice) {
        if (isBlank(clientId) || isBlank(photographerId) || isBlank(date)
                || isBlank(location) || (!fullDay && isBlank(startTime))) {
            throw new IllegalArgumentException("Complete the booking details before continuing.");
        }

        DocumentReference bookingRef = firestore.collection("Booking").document();
        DocumentReference scheduleRef = firestore.collection("daily_schedules").document(date);
        return firestore.runTransaction((Transaction.Function<String>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot schedule = transaction.get(scheduleRef);
            List<String> bookedSlots = new ArrayList<>();
            Object rawSlots = schedule.get("bookedSlots");
            if (rawSlots instanceof List<?>) {
                for (Object slot : (List<?>) rawSlots) if (slot != null) bookedSlots.add(String.valueOf(slot));
            }

            if (fullDay) {
                if (!bookedSlots.isEmpty()) throw new IllegalStateException("That date already has a booking.");
                bookedSlots.add("FULL_DAY");
            } else {
                if (bookedSlots.contains("FULL_DAY") || bookedSlots.contains(startTime)) {
                    throw new IllegalStateException("That time has just been booked. Please choose another time.");
                }
                bookedSlots.add(startTime);
            }

            Map<String, Object> booking = new HashMap<>();
            booking.put("bookingId", bookingRef.getId());
            booking.put("clientId", clientId);
            booking.put("photographerId", photographerId);
            booking.put("packageId", isBlank(packageId) ? null : packageId);
            booking.put("packageName", isBlank(packageName) ? null : packageName);
            booking.put("serviceIds", serviceIds == null ? new ArrayList<>() : serviceIds);
            booking.put("serviceNames", serviceNames == null ? new ArrayList<>() : serviceNames);
            booking.put("addonIds", addonIds == null ? new ArrayList<>() : addonIds);
            booking.put("addonNames", addonNames == null ? new ArrayList<>() : addonNames);
            booking.put("clientName", isBlank(clientName) ? null : clientName.trim());
            booking.put("location", location.trim());
            booking.put("isFullDay", fullDay);
            List<String> dates = new ArrayList<>();
            dates.add(date);
            booking.put("bookedDates", dates);
            booking.put("startTime", fullDay ? null : startTime);
            booking.put("bufferEndTime", fullDay ? null : endTime);
            booking.put("totalPrice", totalPrice);
            booking.put("status", "pending");
            booking.put("createdAt", FieldValue.serverTimestamp());

            Map<String, Object> scheduleData = new HashMap<>();
            scheduleData.put("date", date);
            scheduleData.put("bookedSlots", bookedSlots);
            scheduleData.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(bookingRef, booking);
            transaction.set(scheduleRef, scheduleData, SetOptions.merge());
            return bookingRef.getId();
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
