package com.lld.realworldexamples.ShoppingCart.entities;

public class CartItem {
    private final Product product;
    private final double priceAtAddition;
    private int quantity;

    CartItem(Product product, int quantity, double priceAtAddition) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtAddition = priceAtAddition;
    }

    public double getSubtotal() {
        return quantity * priceAtAddition;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceAtAddition() {
        return priceAtAddition;
    }
}
