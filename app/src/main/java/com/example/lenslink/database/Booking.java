package com.example.lenslink.database;

import java.util.List;

public class Booking {
    private String bookingId;
    private String clientId;
    private String packageId;
    private List<String> addonIds;
    private String location;

    private boolean isFullDay;
    private List<String> bookedDates; // ["2026-11-14", "2026-11-15"]
    private String startTime; // "09:00" (Null if full day)
    private String bufferEndTime; // "11:00" (Null if full day)

    private double totalPrice;
    private String status; // "pending", "confirmed", "completed"

    public Booking() {}

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public List<String> getAddonIds() {
        return addonIds;
    }

    public void setAddonIds(List<String> addonIds) {
        this.addonIds = addonIds;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isFullDay() {
        return isFullDay;
    }

    public void setFullDay(boolean fullDay) {
        isFullDay = fullDay;
    }

    public List<String> getBookedDates() {
        return bookedDates;
    }

    public void setBookedDates(List<String> bookedDates) {
        this.bookedDates = bookedDates;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getBufferEndTime() {
        return bufferEndTime;
    }

    public void setBufferEndTime(String bufferEndTime) {
        this.bufferEndTime = bufferEndTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Add Getters and Setters
}