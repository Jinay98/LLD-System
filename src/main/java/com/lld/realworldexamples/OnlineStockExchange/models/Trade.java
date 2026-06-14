package com.lld.realworldexamples.OnlineStockExchange.models;

import java.util.UUID;

public class Trade {
    private final String tradeId;
    private final User buyer;
    private final User seller;
    private final Stock stock;
    private final int quantity;
    private final double price;

    public Trade(User buyer, User seller, Stock stock, int quantity, double price) {
        this.tradeId = UUID.randomUUID().toString();
        this.buyer = buyer;
        this.seller = seller;
        this.stock = stock;
        this.quantity = quantity;
        this.price = price;
    }

    public String getTradeId() {
        return tradeId;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public Stock getStock() {
        return stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}

