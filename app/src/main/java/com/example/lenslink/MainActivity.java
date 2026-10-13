package com.example.lenslink;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lenslink.database.Booking;
import com.example.lenslink.database.DailySchedule;
import com.example.lenslink.database.FirestoreRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Your default layout

        // Run the test
        runDatabaseTest();
    }

    private void runDatabaseTest() {
        FirestoreRepository repository = new FirestoreRepository();

        // 1. Create a dummy Booking (e.g., an automotive shoot)
        Booking testBooking = new Booking();
        // Assuming you generated standard setters for all the variables in Booking.java
        testBooking.setClientId("client_123");
        testBooking.setPackageId("pkg_automotive_track");
        testBooking.setLocation("Kyalami Grand Prix Circuit");
        testBooking.setFullDay(false);
        testBooking.setBookedDates(Arrays.asList("2026-10-24"));
        testBooking.setStartTime("09:00");
        testBooking.setBufferEndTime("11:00");
        testBooking.setTotalPrice(1500.00);
        testBooking.setStatus("confirmed");

        // 2. Create the Calendar Block for that day
        DailySchedule testSchedule = new DailySchedule();
        testSchedule.setDate("2026-10-24");
        // Blocking out 09:00 for the shoot, and 10:00 for travel/setup buffer
        testSchedule.setBookedSlots(Arrays.asList("09:00", "10:00"));

        List<DailySchedule> scheduleList = new ArrayList<>();
        scheduleList.add(testSchedule);

        // 3. Send it to Firestore
        Toast.makeText(this, "Sending test booking...", Toast.LENGTH_SHORT).show();
        repository.createBooking(testBooking, scheduleList);

        Log.d("FirebaseTest", "Test fired. Check Firebase Console!");
    }
}

