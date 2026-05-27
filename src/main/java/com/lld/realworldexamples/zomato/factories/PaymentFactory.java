package com.lld.realworldexamples.zomato.factories;

import com.lld.realworldexamples.zomato.enums.PaymentStrategies;
import com.lld.realworldexamples.zomato.strategies.payment.IPaymentStrategy;
import com.lld.realworldexamples.zomato.strategies.payment.impl.CreditCardPaymentStrategy;
import com.lld.realworldexamples.zomato.strategies.payment.impl.UPIPaymentStrategy;

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
