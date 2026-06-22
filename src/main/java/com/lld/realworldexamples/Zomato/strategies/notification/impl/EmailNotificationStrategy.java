package com.lld.realworldexamples.Zomato.strategies.notification.impl;

import com.lld.realworldexamples.Zomato.models.Order;
import com.lld.realworldexamples.Zomato.models.User;
import com.lld.realworldexamples.Zomato.strategies.notification.INotificationStrategy;

public class EmailNotificationStrategy implements INotificationStrategy {

    @Override
    public void notify(User user, Order order) {
        System.out.println("Sent Email notification to User:" + user.getName() + " for orderId:" + order.getId());
    }
}
