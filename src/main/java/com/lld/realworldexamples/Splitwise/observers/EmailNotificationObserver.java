package com.lld.realworldexamples.Splitwise.observers;

import com.lld.realworldexamples.Splitwise.entities.Expense;

public class EmailNotificationObserver implements ExpenseObserver {
    @Override
    public void onExpenseAdded(Expense expense) {
        System.out.printf("[Email Notification] New expense: %s ($%.2f) paid by %s, split %s among %d people%n",
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidByUserId(),
                expense.getSplitType(),
                expense.getSplits().size());
    }

    @Override
    public void onSettlement(String fromUserId, String toUserId, double amount) {
        System.out.printf("[Email Notification] Settlement: %s paid %s $%.2f%n",
                fromUserId, toUserId, amount);
    }
}
