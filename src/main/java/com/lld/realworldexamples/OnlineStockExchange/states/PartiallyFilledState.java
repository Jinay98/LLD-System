package com.lld.realworldexamples.OnlineStockExchange.states;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderStatus;
import com.lld.realworldexamples.OnlineStockExchange.models.Order;

public class PartiallyFilledState implements OrderState {
    @Override
    public void cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setState(new CancelledState());
        System.out.println("Order " + order.getOrderId() + " has been cancelled.");
    }
}
