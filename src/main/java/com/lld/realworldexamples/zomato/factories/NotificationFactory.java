package com.lld.realworldexamples.zomato.factories;

import com.lld.realworldexamples.zomato.enums.NotificationStrategies;
import com.lld.realworldexamples.zomato.strategies.notification.INotificationStrategy;
import com.lld.realworldexamples.zomato.strategies.notification.impl.EmailNotificationStrategy;
import com.lld.realworldexamples.zomato.strategies.notification.impl.SMSNotificationStrategy;


public class NotificationFactory {

    public static INotificationStrategy getNotificationStrategy(String strategy) {
        if (strategy.equalsIgnoreCase(NotificationStrategies.SMS.name())) {
            return new SMSNotificationStrategy();
        } else if (strategy.equalsIgnoreCase(NotificationStrategies.EMAIL.name())) {
            return new EmailNotificationStrategy();
        }
        throw new RuntimeException("Invalid strategy entered");
    }
}
