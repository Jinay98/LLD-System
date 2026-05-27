package com.lld.realworldexamples.zomato.strategies.payment.impl;

import com.lld.realworldexamples.zomato.strategies.payment.IPaymentStrategy;

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
