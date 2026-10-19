package com.example.lenslink.model;

import java.util.ArrayList;
import java.util.List;

/** A photographer's bookable photography service. */
public class ServiceOffering {
    private String id;
    private String photographerId;
    private String name;
    private String description;
    private double price;
    private long durationMinutes;
    private List<String> inclusions = new ArrayList<>();
    private List<String> addonIds = new ArrayList<>();
    private boolean active = true;

    public ServiceOffering() { }

    public ServiceOffering(String id, String photographerId, String name, String description,
                           double price, long durationMinutes, List<String> inclusions,
                           List<String> addonIds, boolean active) {
        this.id = id;
        this.photographerId = photographerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.inclusions = inclusions == null ? new ArrayList<>() : inclusions;
        this.addonIds = addonIds == null ? new ArrayList<>() : addonIds;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhotographerId() { return photographerId; }
    public void setPhotographerId(String photographerId) { this.photographerId = photographerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
    public List<String> getInclusions() { return inclusions; }
    public void setInclusions(List<String> inclusions) { this.inclusions = inclusions; }
    public List<String> getAddonIds() { return addonIds; }
    public void setAddonIds(List<String> addonIds) { this.addonIds = addonIds; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
