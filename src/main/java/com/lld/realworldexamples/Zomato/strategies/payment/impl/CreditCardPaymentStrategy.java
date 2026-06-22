package com.lld.realworldexamples.Zomato.strategies.payment.impl;

import com.lld.realworldexamples.Zomato.strategies.payment.IPaymentStrategy;

public class CreditCardPaymentStrategy implements IPaymentStrategy {
    String cardNo;
    public CreditCardPaymentStrategy(String cardNo) {
        this.cardNo = cardNo;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Amount: " + amount + " has been paid via card ending with:" + cardNo);

    }
}
