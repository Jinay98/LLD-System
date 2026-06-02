package com.lld.realworldexamples.Splitwise.strategies;

import com.lld.realworldexamples.Splitwise.entities.split.Split;
import com.lld.realworldexamples.Splitwise.exceptions.InvalidSplitException;

import java.util.List;

public class ExactSplitStrategy implements SplitStrategy {
    @Override
    public void validate(List<Split> splits, double totalAmount) {
        double sum = 0;
        for (Split split : splits) {
            sum += split.getAmount();
        }
        // Use epsilon comparison for floating point
        if (Math.abs(sum - totalAmount) > 0.01) {
            throw new InvalidSplitException(
                    String.format("Exact split amounts (%.2f) don't sum to total (%.2f)", sum, totalAmount));
        }
    }

    @Override
    public void calculateSplits(List<Split> splits, double totalAmount) {
        // Amounts are already set by the caller, nothing to calculate
    }
}
