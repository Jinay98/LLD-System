package com.lld.realworldexamples.ShoppingCart.strategies;

import com.lld.realworldexamples.ShoppingCart.entities.CartItem;
import com.lld.realworldexamples.ShoppingCart.enums.DiscountType;

import java.util.List;

public class FlatAmountDiscountStrategy implements DiscountStrategy {
    private final double amount;

    FlatAmountDiscountStrategy(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Discount amount cannot be negative");
        }
        this.amount = amount;
    }

    @Override
    public double calculateDiscount(List<CartItem> items) {
        double subtotal = items.stream().mapToDouble(CartItem::getSubtotal).sum();
        // Cap at subtotal to prevent negative totals
        return Math.min(amount, subtotal);
    }

    @Override
    public String getDescription() {
        return "$" + String.format("%.2f", amount) + " off";
    }

    @Override
    public DiscountType getType() {
        return DiscountType.FLAT_AMOUNT;
    }
}
