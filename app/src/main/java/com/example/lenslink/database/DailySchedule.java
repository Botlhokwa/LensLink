package com.example.lenslink.database;

import java.util.List;

public class DailySchedule {
    private String date; // Format: YYYY-MM-DD
    private List<String> bookedSlots; // e.g., ["09:00", "10:00"] or ["FULL_DAY"]

    public DailySchedule() {}

    public DailySchedule(String date, List<String> bookedSlots) {
        this.date = date;
        this.bookedSlots = bookedSlots;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(List<String> bookedSlots) {
        this.bookedSlots = bookedSlots;
    }
}
