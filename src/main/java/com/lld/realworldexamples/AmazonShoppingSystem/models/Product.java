package com.lld.realworldexamples.AmazonShoppingSystem.models;

import com.lld.realworldexamples.AmazonShoppingSystem.enums.ProductCategory;

import java.util.UUID;

public class Product {
    protected String id;
    protected String name;
    protected String description;
    protected double price;
    protected ProductCategory category;

    protected Product() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public ProductCategory getCategory() {
        return category;
    }

    // Builder Pattern for creating products
    public static class Builder {
        private final String name;
        private final double price;
        private String description = "";
        private ProductCategory category;

        public Builder(String name, double price) {
            this.name = name;
            this.price = price;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCategory(ProductCategory category) {
            this.category = category;
            return this;
        }

        public Product build() {
            Product product = new Product();
            product.id = UUID.randomUUID().toString();
            product.name = name;
            product.description = description;
            product.price = price;
            product.category = category;
            return product;
        }
    }
}
