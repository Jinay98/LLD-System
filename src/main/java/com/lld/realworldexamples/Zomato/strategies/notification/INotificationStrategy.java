package com.lld.realworldexamples.Zomato.strategies.notification;

import com.lld.realworldexamples.Zomato.models.Order;
import com.lld.realworldexamples.Zomato.models.User;

public interface INotificationStrategy {
    public void notify(User user, Order order);

}
