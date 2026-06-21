package com.lld.realworldexamples.AmazonShoppingSystem.services;

import com.lld.realworldexamples.AmazonShoppingSystem.models.Customer;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Order;
import com.lld.realworldexamples.AmazonShoppingSystem.models.OrderLineItem;
import com.lld.realworldexamples.AmazonShoppingSystem.models.ShoppingCart;

import java.util.List;
import java.util.stream.Collectors;

public class OrderService {
    private final InventoryService inventoryService;

    public OrderService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public Order createOrder(Customer customer, ShoppingCart cart) {
        List<OrderLineItem> orderItems = cart.getItems().values().stream()
                .map(cartItem -> new OrderLineItem(
                        cartItem.getProduct().getId(),
                        cartItem.getProduct().getName(),
                        cartItem.getQuantity(),
                        cartItem.getProduct().getPrice()))
                .collect(Collectors.toList());

        // This is a critical section
        inventoryService.updateStockForOrder(orderItems);

        return new Order(customer, orderItems, customer.getShippingAddress(), cart.calculateTotal());
    }
}
