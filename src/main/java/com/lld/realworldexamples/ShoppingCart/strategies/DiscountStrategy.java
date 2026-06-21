package com.lld.realworldexamples.ShoppingCart.strategies;

import com.lld.realworldexamples.ShoppingCart.entities.CartItem;
import com.lld.realworldexamples.ShoppingCart.enums.DiscountType;

import java.util.List;

public interface DiscountStrategy {
    double calculateDiscount(List<CartItem> items);

    String getDescription();

    DiscountType getType();
}
