package com.example.lenslink.models;
public class Booking {
    private String bookingId, clientId, clientName, photographerId, photographerName, serviceTitle, date, timeSlot, location, status;
    private double totalPrice;
    public Booking() {}
    public Booking(String bookingId, String clientId, String clientName, String photographerId, String photographerName,
                   String serviceTitle, String date, String timeSlot, String location, String status, double totalPrice) {
        this.bookingId = bookingId; this.clientId = clientId; this.clientName = clientName;
        this.photographerId = photographerId; this.photographerName = photographerName;
        this.serviceTitle = serviceTitle; this.date = date; this.timeSlot = timeSlot;
        this.location = location; this.status = status; this.totalPrice = totalPrice;
    }
    public String getBookingId() { return bookingId; } public void setBookingId(String v) { bookingId = v; }
    public String getClientId() { return clientId; } public void setClientId(String v) { clientId = v; }
    public String getClientName() { return clientName; } public void setClientName(String v) { clientName = v; }
    public String getPhotographerId() { return photographerId; } public void setPhotographerId(String v) { photographerId = v; }
    public String getPhotographerName() { return photographerName; } public void setPhotographerName(String v) { photographerName = v; }
    public String getServiceTitle() { return serviceTitle; } public void setServiceTitle(String v) { serviceTitle = v; }
    public String getDate() { return date; } public void setDate(String v) { date = v; }
    public String getTimeSlot() { return timeSlot; } public void setTimeSlot(String v) { timeSlot = v; }
    public String getLocation() { return location; } public void setLocation(String v) { location = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public double getTotalPrice() { return totalPrice; } public void setTotalPrice(double v) { totalPrice = v; }
}
