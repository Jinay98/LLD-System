package com.lld.realworldexamples.OnlineStockExchange.builder;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderType;
import com.lld.realworldexamples.OnlineStockExchange.enums.TransactionType;
import com.lld.realworldexamples.OnlineStockExchange.models.Order;
import com.lld.realworldexamples.OnlineStockExchange.models.Stock;
import com.lld.realworldexamples.OnlineStockExchange.models.User;
import com.lld.realworldexamples.OnlineStockExchange.strategy.LimitOrderStrategy;
import com.lld.realworldexamples.OnlineStockExchange.strategy.MarketOrderStrategy;

import java.util.UUID;

public class OrderBuilder {
    private User user;
    private Stock stock;
    private OrderType type;
    private TransactionType transactionType;
    private int quantity;
    private double price;

    public OrderBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public OrderBuilder withStock(Stock stock) {
        this.stock = stock;
        return this;
    }

    public OrderBuilder buy(int quantity) {
        this.transactionType = TransactionType.BUY;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder sell(int quantity) {
        this.transactionType = TransactionType.SELL;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder atMarketPrice() {
        this.type = OrderType.MARKET;
        this.price = 0; // Not needed for market order
        return this;
    }

    public OrderBuilder withLimit(double limitPrice) {
        this.type = OrderType.LIMIT;
        this.price = limitPrice;
        return this;
    }

    public Order build() {
        return new Order(
                UUID.randomUUID().toString(),
                user,
                stock,
                type,
                transactionType,
                quantity,
                price,
                type == OrderType.MARKET ? new MarketOrderStrategy() : new LimitOrderStrategy()
        );
    }
}

