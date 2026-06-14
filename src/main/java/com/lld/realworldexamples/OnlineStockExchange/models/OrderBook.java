package com.lld.realworldexamples.OnlineStockExchange.models;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderStatus;
import com.lld.realworldexamples.OnlineStockExchange.enums.OrderType;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrderBook {
    private final String symbol;
    private final List<Order> buyOrders = new CopyOnWriteArrayList<>();
    private final List<Order> sellOrders = new CopyOnWriteArrayList<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    private static boolean isActive(Order order) {
        return order.getStatus() == OrderStatus.OPEN || order.getStatus() == OrderStatus.PARTIALLY_FILLED;
    }

    public String getSymbol() {
        return symbol;
    }

    public void addBuyOrder(Order order) {
        buyOrders.add(order);
    }

    public void addSellOrder(Order order) {
        sellOrders.add(order);
    }

    public void removeOrder(Order order) {
        buyOrders.remove(order);
        sellOrders.remove(order);
    }

    // Highest-priced buy among active orders; a market buy outranks every limit buy.
    public Order getBestBuy() {
        return buyOrders.stream()
                .filter(OrderBook::isActive)
                .max(Comparator.comparingDouble(o -> o.getType() == OrderType.MARKET ? Double.MAX_VALUE : o.getPrice()))
                .orElse(null);
    }

    // Lowest-priced sell among active orders; a market sell is matched first.
    public Order getBestSell() {
        return sellOrders.stream()
                .filter(OrderBook::isActive)
                .min(Comparator.comparingDouble(o -> o.getType() == OrderType.MARKET ? 0.0 : o.getPrice()))
                .orElse(null);
    }
}

