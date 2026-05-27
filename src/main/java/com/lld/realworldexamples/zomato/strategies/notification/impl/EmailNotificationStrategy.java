package com.lld.realworldexamples.zomato.strategies.notification.impl;

import com.lld.realworldexamples.zomato.models.Order;
import com.lld.realworldexamples.zomato.models.User;
import com.lld.realworldexamples.zomato.strategies.notification.INotificationStrategy;

public class EmailNotificationStrategy implements INotificationStrategy {

    @Override
    public void notify(User user, Order order) {
        System.out.println("Sent Email notification to User:" + user.getName() + " for orderId:" + order.getId());
    }
}
