package com.lld.realworldexamples.Zomato.factories;

import com.lld.realworldexamples.Zomato.enums.OrderTypes;
import com.lld.realworldexamples.Zomato.models.Cart;
import com.lld.realworldexamples.Zomato.models.CartItem;
import com.lld.realworldexamples.Zomato.models.Order;
import com.lld.realworldexamples.Zomato.models.OrderItem;
import com.lld.realworldexamples.Zomato.models.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderFactory {

    public static Order createOrder(User user, Cart cart, OrderTypes orderType) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderedAt(Instant.now());
        order.setOrderItems(getOrderItems(cart));
        order.setOrderType(orderType);
        order.setRestaurant(cart.getRestaurant());
        order.setStatus("PENDING");
        order.setTotalAmt(getTotalAmount(cart));
        return order;
    }

    private static List<OrderItem> getOrderItems(Cart cart) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem(cartItem.getFoodItem(), cartItem.getQty(),
                    cartItem.getFoodItem().getPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private static double getTotalAmount(Cart cart) {
        double total = 0;
        for (CartItem cartItem : cart.getCartItems()) {
            total += cartItem.getSubTotal();
        }
        return total;
    }
}
