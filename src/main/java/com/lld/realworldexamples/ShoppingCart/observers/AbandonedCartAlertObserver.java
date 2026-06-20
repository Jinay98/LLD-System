package com.lld.realworldexamples.ShoppingCart.observers;

import com.lld.realworldexamples.ShoppingCart.entities.Cart;
import com.lld.realworldexamples.ShoppingCart.entities.CartItem;

public class AbandonedCartAlertObserver implements CartObserver {
    private volatile long lastActivityTime;

    AbandonedCartAlertObserver() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void onItemAdded(Cart cart, CartItem item) {
        lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void onItemRemoved(Cart cart, CartItem item) {
        lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void onCartCheckedOut(Cart cart) {
        // Cart checked out, no longer at risk of abandonment
        lastActivityTime = System.currentTimeMillis();
    }

    long getLastActivityTime() {
        return lastActivityTime;
    }
}
