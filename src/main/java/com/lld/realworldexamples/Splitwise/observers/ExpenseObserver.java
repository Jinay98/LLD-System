package com.lld.realworldexamples.Splitwise.observers;

import com.lld.realworldexamples.Splitwise.entities.Expense;

public interface ExpenseObserver {
    void onExpenseAdded(Expense expense);

    void onSettlement(String fromUserId, String toUserId, double amount);
}
