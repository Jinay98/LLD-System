package com.lld.realworldexamples.ShoppingCart.entities;

import com.lld.realworldexamples.ShoppingCart.enums.CartStatus;
import com.lld.realworldexamples.ShoppingCart.exceptions.CartException;
import com.lld.realworldexamples.ShoppingCart.observers.CartObserver;
import com.lld.realworldexamples.ShoppingCart.strategies.DiscountStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Cart {
    private final String id;
    private final Customer customer;
    private final ConcurrentHashMap<String, CartItem> items;
    private final CopyOnWriteArrayList<CartObserver> observers;
    private volatile CartStatus status;
    private volatile DiscountStrategy discountStrategy;

    public Cart(String id, Customer customer) {
        this.id = id;
        this.customer = customer;
        this.items = new ConcurrentHashMap<>();
        this.status = CartStatus.ACTIVE;
        this.discountStrategy = null;
        this.observers = new CopyOnWriteArrayList<>();
    }

    synchronized void addItem(Product product, int quantity) {
        validateActive();
        if (quantity <= 0) {
            throw new CartException("Quantity must be positive");
        }

        CartItem existing = items.get(product.getId());
        int currentQuantity = (existing != null) ? existing.getQuantity() : 0;
        int newTotal = currentQuantity + quantity;

        if (newTotal > product.getMaxQuantityPerCart()) {
            throw new CartException("Cannot add " + quantity + " units of " + product.getName()
                    + ". Maximum allowed: " + product.getMaxQuantityPerCart()
                    + ", currently in cart: " + currentQuantity);
        }

        if (existing != null) {
            existing.setQuantity(newTotal);
        } else {
            // Capture price at the moment of addition
            existing = new CartItem(product, quantity, product.getPrice());
            items.put(product.getId(), existing);
        }

        notifyItemAdded(existing);
    }

    synchronized void removeItem(String productId) {
        validateActive();
        CartItem removed = items.remove(productId);
        if (removed == null) {
            throw new CartException("Product " + productId + " not found in cart");
        }
        notifyItemRemoved(removed);
    }

    synchronized void updateItemQuantity(String productId, int newQuantity) {
        validateActive();
        CartItem item = items.get(productId);
        if (item == null) {
            throw new CartException("Product " + productId + " not found in cart");
        }
        if (newQuantity <= 0) {
            removeItem(productId);
            return;
        }
        if (newQuantity > item.getProduct().getMaxQuantityPerCart()) {
            throw new CartException("Cannot set quantity to " + newQuantity
                    + ". Maximum allowed: " + item.getProduct().getMaxQuantityPerCart());
        }
        item.setQuantity(newQuantity);
    }

    synchronized void applyDiscount(DiscountStrategy strategy) {
        validateActive();
        this.discountStrategy = strategy;
    }

    synchronized void removeDiscount() {
        validateActive();
        this.discountStrategy = null;
    }

    synchronized void checkout() {
        validateActive();
        if (items.isEmpty()) {
            throw new CartException("Cannot checkout an empty cart");
        }
        this.status = CartStatus.CHECKED_OUT;
        notifyCheckedOut();
    }

    synchronized void abandon() {
        validateActive();
        this.status = CartStatus.ABANDONED;
    }

    double getSubtotal() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public double getTotal() {
        double subtotal = getSubtotal();
        if (discountStrategy != null) {
            double discount = discountStrategy.calculateDiscount(new ArrayList<>(items.values()));
            return Math.max(0, subtotal - discount);
        }
        return subtotal;
    }

    void addObserver(CartObserver observer) {
        observers.add(observer);
    }

    private void validateActive() {
        if (status != CartStatus.ACTIVE) {
            throw new CartException("Cannot modify cart. Cart status is " + status);
        }
    }

    private void notifyItemAdded(CartItem item) {
        for (CartObserver observer : observers) {
            observer.onItemAdded(this, item);
        }
    }

    private void notifyItemRemoved(CartItem item) {
        for (CartObserver observer : observers) {
            observer.onItemRemoved(this, item);
        }
    }

    private void notifyCheckedOut() {
        for (CartObserver observer : observers) {
            observer.onCartCheckedOut(this);
        }
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public CartStatus getStatus() {
        return status;
    }

    Map<String, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
