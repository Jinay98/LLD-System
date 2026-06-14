package com.lld.realworldexamples.OnlineStockExchange.models;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderStatus;
import com.lld.realworldexamples.OnlineStockExchange.enums.OrderType;
import com.lld.realworldexamples.OnlineStockExchange.enums.TransactionType;
import com.lld.realworldexamples.OnlineStockExchange.states.OpenState;
import com.lld.realworldexamples.OnlineStockExchange.states.OrderState;
import com.lld.realworldexamples.OnlineStockExchange.strategy.ExecutionStrategy;

public class Order {
    private final String orderId;
    private final User user;
    private final Stock stock;
    private final OrderType type;
    private final TransactionType transactionType;
    private final int quantity;
    private final double price; // Limit price for Limit orders
    private final ExecutionStrategy executionStrategy;
    private OrderStatus status;
    private int filledQuantity; // Shares already traded; the remainder stays on the book
    private OrderState currentState;

    public Order(String orderId, User user, Stock stock, OrderType type, TransactionType transactionType,
                 int quantity, double price, ExecutionStrategy strategy) {
        this.orderId = orderId;
        this.user = user;
        this.stock = stock;
        this.type = type;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.executionStrategy = strategy;
        this.currentState = new OpenState(); // Initial state
        this.status = OrderStatus.OPEN;
        this.filledQuantity = 0;
    }

    // State pattern methods
    public void cancel() {
        currentState.cancel(this);
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public User getUser() {
        return user;
    }

    public Stock getStock() {
        return stock;
    }

    public OrderType getType() {
        return type;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public double getPrice() {
        return price;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        notifyUser();
    }

    public ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    // Setters for state transitions
    public void setState(OrderState state) {
        this.currentState = state;
    }

    public void addFilledQuantity(int quantityTraded) {
        this.filledQuantity += quantityTraded;
    }

    private void notifyUser() {
        user.orderStatusUpdate(this);
    }
}

