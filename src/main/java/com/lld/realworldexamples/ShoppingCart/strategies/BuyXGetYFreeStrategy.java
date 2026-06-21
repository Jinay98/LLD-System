package com.lld.realworldexamples.ShoppingCart.strategies;

import com.lld.realworldexamples.ShoppingCart.entities.CartItem;
import com.lld.realworldexamples.ShoppingCart.enums.DiscountType;
import com.lld.realworldexamples.ShoppingCart.enums.ProductCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuyXGetYFreeStrategy implements DiscountStrategy {
    private final int buyCount;
    private final int freeCount;
    private final ProductCategory category;

    BuyXGetYFreeStrategy(int buyCount, int freeCount, ProductCategory category) {
        if (buyCount <= 0 || freeCount <= 0) {
            throw new IllegalArgumentException("Buy and free counts must be positive");
        }
        this.buyCount = buyCount;
        this.freeCount = freeCount;
        this.category = category;
    }

    @Override
    public double calculateDiscount(List<CartItem> items) {
        // Expand items in the target category into individual units, sorted by price ascending
        List<Double> eligiblePrices = new ArrayList<>();
        for (CartItem item : items) {
            if (item.getProduct().getCategory() == category) {
                for (int i = 0; i < item.getQuantity(); i++) {
                    eligiblePrices.add(item.getPriceAtAddition());
                }
            }
        }
        Collections.sort(eligiblePrices);

        double discount = 0;
        int groupSize = buyCount + freeCount;
        // In each group of (buyCount + freeCount) items, the cheapest freeCount are free
        for (int i = 0; i < eligiblePrices.size(); i++) {
            int positionInGroup = i % groupSize;
            if (positionInGroup < freeCount) {
                discount += eligiblePrices.get(i);
            }
        }
        return discount;
    }

    @Override
    public String getDescription() {
        return "Buy " + buyCount + " Get " + freeCount + " Free (" + category + ")";
    }

    @Override
    public DiscountType getType() {
        return DiscountType.BUY_X_GET_Y_FREE;
    }
}
