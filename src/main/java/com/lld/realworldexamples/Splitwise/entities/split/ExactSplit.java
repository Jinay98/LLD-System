package com.lld.realworldexamples.Splitwise.entities.split;

public class ExactSplit extends Split {
    public ExactSplit(String userId, double amount) {
        super(userId);
        setAmount(amount);
    }
}
