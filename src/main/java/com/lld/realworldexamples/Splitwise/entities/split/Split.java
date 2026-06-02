package com.lld.realworldexamples.Splitwise.entities.split;

public class Split {
    private final String userId;
    private double amount;

    public Split(String userId) {
        this.userId = userId;
        this.amount = 0;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
