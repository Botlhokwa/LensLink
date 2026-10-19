package com.example.lenslink.model;

/** An optional priced extra offered by a photographer. */
public class AddonOffering {
    private String id;
    private String photographerId;
    private String category;
    private String name;
    private String description;
    private double price;
    private boolean active = true;

    public AddonOffering() { }

    public AddonOffering(String id, String photographerId, String name, String description,
                         double price, boolean active) {
        this(id, photographerId, "", name, description, price, active);
    }

    public AddonOffering(String id, String photographerId, String category, String name,
                         String description, double price, boolean active) {
        this.id = id;
        this.photographerId = photographerId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
