package com.lld.realworldexamples.OnlineStockExchange.states;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class FilledState implements OrderState {
    @Override
    public void cancel(Order order) {
        System.out.println("Cannot cancel a filled order.");
    }
}

