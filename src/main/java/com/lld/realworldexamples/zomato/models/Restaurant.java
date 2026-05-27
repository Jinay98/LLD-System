package com.lld.realworldexamples.zomato.models;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private int id;
    private String name;
    private String location;
    private List<FoodItem> menu = new ArrayList<>();

    public Restaurant() {
    }

    public Restaurant(int id, String name, String location, List<FoodItem> menu) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.menu = menu;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<FoodItem> getMenu() {
        return menu;
    }

    public void setMenu(List<FoodItem> menu) {
        this.menu = menu;
    }
}
