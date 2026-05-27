package com.lld.realworldexamples.zomato.strategies.notification;

import com.lld.realworldexamples.zomato.models.Order;
import com.lld.realworldexamples.zomato.models.User;

public interface INotificationStrategy {
    public void notify(User user, Order order);

}
