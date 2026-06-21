package com.lld.realworldexamples.AmazonShoppingSystem.states;

import com.lld.realworldexamples.AmazonShoppingSystem.models.Order;

public interface OrderState {
    void ship(Order order);

    void deliver(Order order);

    void cancel(Order order);
}
