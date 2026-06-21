package com.lld.realworldexamples.AmazonShoppingSystem.observers;

import com.lld.realworldexamples.AmazonShoppingSystem.models.Order;

public interface OrderObserver {
    void update(Order order);
}
