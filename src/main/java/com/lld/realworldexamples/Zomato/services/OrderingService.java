package com.lld.realworldexamples.Zomato.services;

import com.lld.realworldexamples.Zomato.enums.OrderTypes;
import com.lld.realworldexamples.Zomato.factories.NotificationFactory;
import com.lld.realworldexamples.Zomato.factories.OrderFactory;
import com.lld.realworldexamples.Zomato.factories.PaymentFactory;
import com.lld.realworldexamples.Zomato.managers.OrderManager;
import com.lld.realworldexamples.Zomato.models.Cart;
import com.lld.realworldexamples.Zomato.models.Order;
import com.lld.realworldexamples.Zomato.models.User;
import com.lld.realworldexamples.Zomato.strategies.notification.INotificationStrategy;
import com.lld.realworldexamples.Zomato.strategies.payment.IPaymentStrategy;

public class OrderingService {

    public static Order placeOrder(User user, Cart cart, OrderTypes orderType,
                                   String paymentStrategyType, String cardDetails,
                                   String notificationStrategyType) {

        // Step 1: Create order from cart
        System.out.println("\n=== Step 1: Creating Order ===");
        Order order = OrderFactory.createOrder(user, cart, orderType);
        System.out.println("Order created with total amount: " + order.getTotalAmt());

        // Step 2: Process payment
        System.out.println("\n=== Step 2: Processing Payment ===");
        IPaymentStrategy paymentStrategy = PaymentFactory.getPaymentStrategy(paymentStrategyType, cardDetails, null);
        paymentStrategy.pay(order.getTotalAmt());
        order.setStatus("CONFIRMED");
        System.out.println("Payment successful!");

        // Step 3: Save order
        System.out.println("\n=== Step 3: Saving Order ===");
        OrderManager.getInstance().saveOrder(order);

        // Step 4: Send notification
        System.out.println("\n=== Step 4: Sending Notification ===");
        INotificationStrategy notificationStrategy = NotificationFactory.getNotificationStrategy(notificationStrategyType);
        notificationStrategy.notify(user, order);

        // Step 5: Clear cart
        System.out.println("\n=== Step 5: Clearing Cart ===");
        user.getCartManager().clearCart(cart);
        System.out.println("Cart cleared!");

        return order;
    }
}
