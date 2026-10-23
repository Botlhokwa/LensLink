package com.example.lenslink.models;
public class Service {
    private String serviceId, studioId, photographerName, name, category, description, imageUrl;
    private double basePrice;
    public Service() {}
    public Service(String serviceId, String studioId, String photographerName, String name, String category, String description, double basePrice, String imageUrl) {
        this.serviceId = serviceId; this.studioId = studioId; this.photographerName = photographerName;
        this.name = name; this.category = category; this.description = description; this.basePrice = basePrice; this.imageUrl = imageUrl;
    }
    public String getServiceId() { return serviceId; } public void setServiceId(String v) { serviceId = v; }
    public String getStudioId() { return studioId; } public void setStudioId(String v) { studioId = v; }
    public String getPhotographerName() { return photographerName; } public void setPhotographerName(String v) { photographerName = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public double getBasePrice() { return basePrice; } public void setBasePrice(double v) { basePrice = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
}
