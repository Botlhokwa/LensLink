package com.example.lenslink.models;
public class Package {
    private String packageId, serviceId, photographerName, name, inclusions;
    private double price;
    private int durationHours;
    public Package() {}
    public Package(String packageId, String serviceId, String photographerName, String name, double price, int durationHours, String inclusions) {
        this.packageId = packageId; this.serviceId = serviceId; this.photographerName = photographerName;
        this.name = name; this.price = price; this.durationHours = durationHours; this.inclusions = inclusions;
    }
    public String getPackageId() { return packageId; } public void setPackageId(String v) { packageId = v; }
    public String getServiceId() { return serviceId; } public void setServiceId(String v) { serviceId = v; }
    public String getPhotographerName() { return photographerName; } public void setPhotographerName(String v) { photographerName = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public double getPrice() { return price; } public void setPrice(double v) { price = v; }
    public int getDurationHours() { return durationHours; } public void setDurationHours(int v) { durationHours = v; }
    public String getInclusions() { return inclusions; } public void setInclusions(String v) { inclusions = v; }
}
