package com.lld.realworldexamples.AmazonShoppingSystem.services;

import com.lld.realworldexamples.AmazonShoppingSystem.strategies.PaymentStrategy;

public class PaymentService {
    public boolean processPayment(PaymentStrategy strategy, double amount) {
        return strategy.pay(amount);
    }
}
