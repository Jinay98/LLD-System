package com.lld.realworldexamples.Zomato.models;

import com.lld.realworldexamples.Zomato.enums.OrderTypes;

import java.time.Instant;
import java.util.List;

public class Order {
    private int id;
    private User user;
    private Restaurant restaurant;
    private OrderTypes orderType;
    private List<OrderItem> orderItems;
    private double totalAmt;
    private String status;
    private Instant orderedAt;

    public Order() {
    }

    public Order(int id, User user, Restaurant restaurant, OrderTypes orderType, List<OrderItem> orderItems, double totalAmt, String status, Instant orderedAt) {
        this.id = id;
        this.user = user;
        this.restaurant = restaurant;
        this.orderType = orderType;
        this.orderItems = orderItems;
        this.totalAmt = totalAmt;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public OrderTypes getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderTypes orderType) {
        this.orderType = orderType;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public double getTotalAmt() {
        return totalAmt;
    }

    public void setTotalAmt(double totalAmt) {
        this.totalAmt = totalAmt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(Instant orderedAt) {
        this.orderedAt = orderedAt;
    }


}
