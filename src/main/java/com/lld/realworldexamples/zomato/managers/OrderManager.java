package com.lld.realworldexamples.zomato.managers;

import com.lld.realworldexamples.zomato.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderManager {
    private static OrderManager instance;
    private final List<Order> orders = new ArrayList<>();
    private int orderIdCounter = 1;

    public static OrderManager getInstance() {
        if (instance == null) {
            instance = new OrderManager();
        }
        return instance;
    }

    public void saveOrder(Order order) {
        order.setId(orderIdCounter++);
        orders.add(order);
        System.out.println("Order saved with ID: " + order.getId());
    }

    public Order getOrderById(int id) {
        for (Order order : orders) {
            if (order.getId() == id) return order;
        }
        return null;
    }
}
