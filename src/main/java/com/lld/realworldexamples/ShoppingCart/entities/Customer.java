package com.lld.realworldexamples.ShoppingCart.entities;

public class Customer {
    private final String id;
    private final String name;
    private final String email;

    Customer(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }
}
