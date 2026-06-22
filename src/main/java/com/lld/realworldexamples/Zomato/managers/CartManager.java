package com.lld.realworldexamples.Zomato.managers;

import com.lld.realworldexamples.Zomato.models.Cart;
import com.lld.realworldexamples.Zomato.models.CartItem;
import com.lld.realworldexamples.Zomato.models.FoodItem;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    public void addFoodItemToCart(Cart cart, FoodItem foodItem, int qty) {
        boolean isNewFoodItem = true;
        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getFoodItem().getCode().equalsIgnoreCase(foodItem.getCode())) {
                isNewFoodItem = false;
                cartItem.setQty(cartItem.getQty() + qty);
                break;
            }
        }
        if (isNewFoodItem) {
            CartItem cartItem = new CartItem(foodItem, qty);
            cart.getCartItems().add(cartItem);
        }
    }

    public void removeFoodItemFromCart(Cart cart, FoodItem foodItem) {
        List<CartItem> cartItems = cart.getCartItems().stream().filter(
                cartItem -> !cartItem.getFoodItem().getCode().
                        equalsIgnoreCase(foodItem.getCode())).toList();
        cart.setCartItems(cartItems);
    }

    public void clearCart(Cart cart) {
        List<CartItem> cartItems = new ArrayList<>();
        cart.setCartItems(cartItems);
    }

    public double getTotal(Cart cart) {
        double total = 0;
        for (CartItem cartItem : cart.getCartItems()) {
            total += cartItem.getSubTotal();
        }
        return total;
    }

    public List<CartItem> getCartItems(Cart cart) {
        return cart.getCartItems();
    }
}
