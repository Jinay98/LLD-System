package com.lld.realworldexamples.ShoppingCart.entities;

import com.lld.realworldexamples.ShoppingCart.enums.ProductCategory;

public class Product {
    private final String id;
    private final String name;
    private final double price;
    private final ProductCategory category;
    private final int maxQuantityPerCart;

    Product(String id, String name, double price, ProductCategory category, int maxQuantityPerCart) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        if (maxQuantityPerCart <= 0) throw new IllegalArgumentException("Max quantity must be positive");
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.maxQuantityPerCart = maxQuantityPerCart;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    double getPrice() {
        return price;
    }

    public ProductCategory getCategory() {
        return category;
    }

    int getMaxQuantityPerCart() {
        return maxQuantityPerCart;
    }
}
