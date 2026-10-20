package com.example.lenslink.model;

import java.util.ArrayList;
import java.util.List;

/** A photographer's priced bundle of services and optional add-ons. */
public class PackageOffering {
    private String id;
    private String photographerId;
    private String category;
    private String name;
    private String description;
    private double price;
    private double twoDayPrice;
    private double partnerPrice;
    private List<String> serviceIds = new ArrayList<>();
    private List<String> inclusions = new ArrayList<>();
    private List<String> addonIds = new ArrayList<>();
    private boolean active = true;

    public PackageOffering() { }

    public PackageOffering(String id, String photographerId, String name, String description,
                           double price, List<String> serviceIds, List<String> inclusions,
                           List<String> addonIds, boolean active) {
        this(id, photographerId, "", name, description, price, 0, 0, serviceIds, inclusions, addonIds, active);
    }

    public PackageOffering(String id, String photographerId, String category, String name,
                           String description, double price, double twoDayPrice, double partnerPrice,
                           List<String> serviceIds, List<String> inclusions,
                           List<String> addonIds, boolean active) {
        this.id = id;
        this.photographerId = photographerId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.twoDayPrice = twoDayPrice;
        this.partnerPrice = partnerPrice;
        this.serviceIds = serviceIds == null ? new ArrayList<>() : serviceIds;
        this.inclusions = inclusions == null ? new ArrayList<>() : inclusions;
        this.addonIds = addonIds == null ? new ArrayList<>() : addonIds;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhotographerId() { return photographerId; }
    public void setPhotographerId(String photographerId) { this.photographerId = photographerId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getTwoDayPrice() { return twoDayPrice; }
    public void setTwoDayPrice(double twoDayPrice) { this.twoDayPrice = twoDayPrice; }
    public double getPartnerPrice() { return partnerPrice; }
    public void setPartnerPrice(double partnerPrice) { this.partnerPrice = partnerPrice; }
    public List<String> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<String> serviceIds) { this.serviceIds = serviceIds; }
    public List<String> getInclusions() { return inclusions; }
    public void setInclusions(List<String> inclusions) { this.inclusions = inclusions; }
    public List<String> getAddonIds() { return addonIds; }
    public void setAddonIds(List<String> addonIds) { this.addonIds = addonIds; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
