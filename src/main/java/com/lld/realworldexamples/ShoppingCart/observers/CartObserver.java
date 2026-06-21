package com.lld.realworldexamples.ShoppingCart.observers;

import com.lld.realworldexamples.ShoppingCart.entities.Cart;
import com.lld.realworldexamples.ShoppingCart.entities.CartItem;

public interface CartObserver {
    void onItemAdded(Cart cart, CartItem item);

    void onItemRemoved(Cart cart, CartItem item);

    void onCartCheckedOut(Cart cart);
}
