package com.lld.realworldexamples.OnlineStockExchange.states;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class FailedState implements OrderState {
    @Override
    public void cancel(Order order) {
        System.out.println("Cannot cancel a failed order.");
    }
}

