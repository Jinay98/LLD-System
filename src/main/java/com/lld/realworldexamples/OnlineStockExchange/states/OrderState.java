package com.lld.realworldexamples.OnlineStockExchange.states;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public interface OrderState {
    void cancel(Order order);
}

