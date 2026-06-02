package com.lld.realworldexamples.Splitwise.strategies;

import com.lld.realworldexamples.Splitwise.entities.split.Split;

import java.util.List;

public interface SplitStrategy {
    void validate(List<Split> splits, double totalAmount);

    void calculateSplits(List<Split> splits, double totalAmount);
}
