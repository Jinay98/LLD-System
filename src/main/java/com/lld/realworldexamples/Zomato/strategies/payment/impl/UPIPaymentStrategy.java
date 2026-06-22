package com.lld.realworldexamples.Zomato.strategies.payment.impl;

import com.lld.realworldexamples.Zomato.strategies.payment.IPaymentStrategy;

public class UPIPaymentStrategy implements IPaymentStrategy {
    String mobileNo;

    public UPIPaymentStrategy(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    @Override
    public void pay(double amount) {
        System.out.println("Amount: " + amount + " has been paid via UPI ID:" + mobileNo + "@okhdfcbank");
    }
}
