package com.example.lenslink.repositories;
import com.example.lenslink.models.Booking;
import com.example.lenslink.utils.FirestoreConstants;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BookingRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final List<Booking> bookings = new ArrayList<>();
    static {
        bookings.add(new Booking("b1", "client1", "Bee Lemme", "photo1", "Barred Production Studios",
            "Golden Hour Outdoor Session", "24/10/2026", "04:30 PM", "Rosebank, Johannesburg", FirestoreConstants.STATUS_CONFIRMED, 2500));
        bookings.add(new Booking("b2", "client1", "Bee Lemme", "photo1", "Barred Production Studios",
            "Graduation Shoot (Standard)", "17/10/2026", "02:00 PM", "Midrand Studio", FirestoreConstants.STATUS_PENDING, 1500));
        bookings.add(new Booking("b3", "client1", "Bee Lemme", "photo2", "Kagiso Visuals",
            "Wedding Shoot", "24/05/2026", "09:00 AM", "Sandton, Johannesburg", FirestoreConstants.STATUS_CONFIRMED, 4500));
    }

    public Set<String> getLockedSlots(String photographerName, String date) {
        Set<String> locked = new HashSet<>();
        for (Booking b : bookings) {
            if (b.getPhotographerName().equalsIgnoreCase(photographerName)
                && b.getDate().equals(date)
                && !FirestoreConstants.STATUS_CANCELLED.equalsIgnoreCase(b.getStatus())) {
                locked.add(b.getTimeSlot());
            }
        }
        return locked;
    }

    public void createBooking(Booking b, RepositoryCallback<Booking> cb) {
        Set<String> locked = getLockedSlots(b.getPhotographerName(), b.getDate());
        if (locked.contains(b.getTimeSlot())) {
            cb.onError(new Exception("Time slot " + b.getTimeSlot() + " on " + b.getDate() + " is already booked for " + b.getPhotographerName() + "! Choose another slot or date."));
            return;
        }
        String id = "bk_" + System.currentTimeMillis();
        b.setBookingId(id);
        bookings.add(0, b);
        cb.onSuccess(b);
    }


    public void getBookingsForClient(String clientName, RepositoryCallback<List<Booking>> cb) {
        List<Booking> res = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getClientName() != null && b.getClientName().equalsIgnoreCase(clientName)) res.add(b);
        }
        cb.onSuccess(res);
    }

    public void getBookingsForPhotographer(String studioName, RepositoryCallback<List<Booking>> cb) {
        List<Booking> res = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getPhotographerName() != null && b.getPhotographerName().equalsIgnoreCase(studioName)) res.add(b);
        }
        cb.onSuccess(res);
    }

    /** Booking date + slot as epoch millis, or -1 when it can't be parsed. */
    public static long whenMillis(Booking b) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US);
            return f.parse(b.getDate() + " " + b.getTimeSlot()).getTime();
        } catch (ParseException | NullPointerException e) {
            return -1;
        }
    }

    /** Soonest non-cancelled booking that is still ahead of now. */
    public static Booking nextUpcoming(List<Booking> list) {
        long now = System.currentTimeMillis();
        Booking best = null;
        long bestTime = Long.MAX_VALUE;
        for (Booking b : list) {
            if (FirestoreConstants.STATUS_CANCELLED.equalsIgnoreCase(b.getStatus())) continue;
            long t = whenMillis(b);
            if (t >= now && t < bestTime) { best = b; bestTime = t; }
        }
        return best;
    }

    public void getAllBookings(RepositoryCallback<List<Booking>> cb) {
        cb.onSuccess(new ArrayList<>(bookings));
    }

    public void updateStatus(String bookingId, String newStatus, RepositoryCallback<Void> cb) {
        for (Booking b : bookings) {
            if (b.getBookingId().equals(bookingId)) {
                b.setStatus(newStatus);
                break;
            }
        }
        cb.onSuccess(null);
    }
}
