package com.lld.realworldexamples.Zomato.factories;

import com.lld.realworldexamples.Zomato.enums.PaymentStrategies;
import com.lld.realworldexamples.Zomato.strategies.payment.IPaymentStrategy;
import com.lld.realworldexamples.Zomato.strategies.payment.impl.CreditCardPaymentStrategy;
import com.lld.realworldexamples.Zomato.strategies.payment.impl.UPIPaymentStrategy;

public class PaymentFactory {
    public static IPaymentStrategy getPaymentStrategy(String strategy, String cardNo, String mobileNo) {
        IPaymentStrategy paymentStrategy;
        if (strategy.equalsIgnoreCase(PaymentStrategies.CREDIT_CARD.name())) {
            paymentStrategy = new CreditCardPaymentStrategy(cardNo);
            return paymentStrategy;
        } else if (strategy.equalsIgnoreCase(PaymentStrategies.UPI.name())) {
            paymentStrategy = new UPIPaymentStrategy(mobileNo);
            return paymentStrategy;
        } else {
            throw new RuntimeException("Payment Strategy is no supported");
        }
    }
}
