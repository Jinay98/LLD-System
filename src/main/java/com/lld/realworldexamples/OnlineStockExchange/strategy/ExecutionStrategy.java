package com.lld.realworldexamples.OnlineStockExchange.strategy;

import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public interface ExecutionStrategy {
    boolean canExecute(Order order, double marketPrice);
}
