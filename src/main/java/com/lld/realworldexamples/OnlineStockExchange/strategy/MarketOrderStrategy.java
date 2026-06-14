package com.lld.realworldexamples.OnlineStockExchange.strategy;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class MarketOrderStrategy implements ExecutionStrategy {
    @Override
    public boolean canExecute(Order order, double marketPrice) {
        return true; // Market orders can always execute
    }
}
