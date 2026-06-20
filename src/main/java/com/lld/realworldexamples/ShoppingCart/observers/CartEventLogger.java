package com.lld.realworldexamples.ShoppingCart.observers;

import com.lld.realworldexamples.ShoppingCart.entities.CartItem;
import com.lld.realworldexamples.ShoppingCart.entities.Cart;

public class CartEventLogger implements CartObserver {
    @Override
    public void onItemAdded(Cart cart, CartItem item) {
        System.out.println("[LOG] Item added to cart " + cart.getId() + ": "
                + item.getQuantity() + "x " + item.getProduct().getName()
                + " @ $" + String.format("%.2f", item.getPriceAtAddition()));
    }

    @Override
    public void onItemRemoved(Cart cart, CartItem item) {
        System.out.println("[LOG] Item removed from cart " + cart.getId() + ": "
                + item.getProduct().getName());
    }

    @Override
    public void onCartCheckedOut(Cart cart) {
        System.out.println("[LOG] Cart " + cart.getId() + " checked out. Total: $"
                + String.format("%.2f", cart.getTotal()));
    }
}
