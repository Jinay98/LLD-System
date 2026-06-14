package com.lld.realworldexamples.OnlineStockExchange.strategy;

import com.lld.realworldexamples.OnlineStockExchange.enums.TransactionType;
import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class LimitOrderStrategy implements ExecutionStrategy {
    @Override
    public boolean canExecute(Order order, double marketPrice) {
        if (order.getTransactionType() == TransactionType.BUY) {
            // Buy if market price is less than or equal to limit price
            return marketPrice <= order.getPrice();
        } else { // SELL
            // Sell if market price is greater than or equal to limit price
            return marketPrice >= order.getPrice();
        }
    }
}
