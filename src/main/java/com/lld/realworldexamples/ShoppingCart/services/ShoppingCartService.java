package com.lld.realworldexamples.ShoppingCart.services;

import com.lld.realworldexamples.ShoppingCart.entities.Cart;
import com.lld.realworldexamples.ShoppingCart.entities.Customer;
import com.lld.realworldexamples.ShoppingCart.enums.CartStatus;
import com.lld.realworldexamples.ShoppingCart.exceptions.CartException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ShoppingCartService {
    private static final Object lock = new Object();
    private static volatile ShoppingCartService instance;
    private final ConcurrentHashMap<String, Cart> carts;
    private final AtomicInteger cartCounter;

    private ShoppingCartService() {
        this.carts = new ConcurrentHashMap<>();
        this.cartCounter = new AtomicInteger(0);
    }

    static ShoppingCartService getInstance() {
        // Double-checked locking: first check avoids acquiring the lock
        // on every call after initialization
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ShoppingCartService();
                }
            }
        }
        return instance;
    }

    Cart createCart(Customer customer) {
        // Check if customer already has an active cart
        Cart existing = getActiveCartForCustomer(customer.getId());
        if (existing != null) {
            return existing;
        }
        String cartId = "CART-" + cartCounter.incrementAndGet();
        Cart cart = new Cart(cartId, customer);
        carts.put(cartId, cart);
        return cart;
    }

    Cart getCart(String cartId) {
        Cart cart = carts.get(cartId);
        if (cart == null) {
            throw new CartException("Cart " + cartId + " not found");
        }
        return cart;
    }

    Cart getActiveCartForCustomer(String customerId) {
        for (Cart cart : carts.values()) {
            if (cart.getCustomer().getId().equals(customerId)
                    && cart.getStatus() == CartStatus.ACTIVE) {
                return cart;
            }
        }
        return null;
    }
}
