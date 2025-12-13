package com.tank2d.tankverse.entity;

import java.util.HashMap;
import java.util.Map;

public class ShopItem {
    public int id;
    public String name;
    public String description;
    public int price;
    public double discount;
    public int stock;
    
    // ✅ Dynamic attributes: {"hp": 50, "mp": 30, "spd": 5, "dmg": 10}
    public Map<String, Double> attributes = new HashMap<>();

    public ShopItem(int id, String name, String description, int price,
                    double discount, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discount = discount;
        this.stock = stock;
    }

    // Constructor with attributes
    public ShopItem(int id, String name, String description, int price,
                    double discount, int stock, Map<String, Double> attributes) {
        this(id, name, description, price, discount, stock);
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

    public int getFinalPrice() {
        return (int)(price * (1 - discount));
    }
    
    // Helper methods
    public double getAttribute(String name) {
        return attributes.getOrDefault(name, 0.0);
    }
    
    public void setAttribute(String name, double value) {
        attributes.put(name, value);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Double> attributes) {
        this.attributes = attributes;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }
}
