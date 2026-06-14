package com.lld.realworldexamples.OnlineStockExchange.states;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class CancelledState implements OrderState {
    @Override
    public void cancel(Order order) {
        System.out.println("Order is already cancelled.");
    }
}

