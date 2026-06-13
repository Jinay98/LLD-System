package com.lld.realworldexamples.MovieBookingSystem.strategies;

import com.lld.realworldexamples.MovieBookingSystem.entities.Payment;

public interface PaymentStrategy {
    Payment pay(double amount);

    void refund(Payment payment);
}
