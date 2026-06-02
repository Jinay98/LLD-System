package com.lld.realworldexamples.Splitwise.strategies;

import com.lld.realworldexamples.Splitwise.entities.split.PercentageSplit;
import com.lld.realworldexamples.Splitwise.entities.split.Split;
import com.lld.realworldexamples.Splitwise.exceptions.InvalidSplitException;

import java.util.List;

public class PercentageSplitStrategy implements SplitStrategy {
    @Override
    public void validate(List<Split> splits, double totalAmount) {
        double totalPercentage = 0;
        for (Split split : splits) {
            if (!(split instanceof PercentageSplit)) {
                throw new InvalidSplitException("All splits must be PercentageSplit for percentage split type");
            }
            totalPercentage += ((PercentageSplit) split).getPercentage();
        }
        if (Math.abs(totalPercentage - 100.0) > 0.01) {
            throw new InvalidSplitException(
                    String.format("Percentages (%.2f%%) don't sum to 100%%", totalPercentage));
        }
    }

    @Override
    public void calculateSplits(List<Split> splits, double totalAmount) {
        double allocated = 0;
        for (int i = 0; i < splits.size() - 1; i++) {
            PercentageSplit ps = (PercentageSplit) splits.get(i);
            double amount = Math.round(totalAmount * ps.getPercentage() / 100.0 * 100.0) / 100.0;
            ps.setAmount(amount);
            allocated += amount;
        }
        // Last person gets the remainder to handle rounding
        double remainder = Math.round((totalAmount - allocated) * 100.0) / 100.0;
        splits.get(splits.size() - 1).setAmount(remainder);
    }
}
