package com.lld.realworldexamples.Splitwise.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceSheet {
    // balances[A][B] = how much A owes B (positive = owes, negative = is owed)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> balances;

    public BalanceSheet() {
        this.balances = new ConcurrentHashMap<>();
    }

    // Increases the debt from fromUserId to toUserId by amount.
    // Example: updateBalance("A", "B", 100)
    // means A owes B 100, and B is owed 100 by A.
    public synchronized void updateBalance(String fromUserId, String toUserId, double amount) {
        recordDebt(fromUserId, toUserId, amount);
        recordDebt(toUserId, fromUserId, -amount);
    }

    // Reduces how much fromUserId owes toUserId.
    // Settlement is represented as the opposite of adding debt.
    public synchronized void settleUp(String fromUserId, String toUserId, double amount) {
        double settlementAmount = -amount;
        updateBalance(fromUserId, toUserId, settlementAmount);
    }

    // Returns how much userId1 owes userId2.
    // Positive = userId1 owes userId2.
    // Negative = userId2 owes userId1.
    public double getBalance(String userId1, String userId2) {
        ConcurrentHashMap<String, Double> userBalances = balances.get(userId1);
        if (userBalances == null) {
            return 0.0;
        }
        return userBalances.getOrDefault(userId2, 0.0);
    }

    // Returns a snapshot of all balances for a user (defensive copy)
    public Map<String, Double> getBalancesForUser(String userId) {
        ConcurrentHashMap<String, Double> userBalances = balances.get(userId);
        if (userBalances == null) {
            return new HashMap<>();
        }
        return new HashMap<>(userBalances);
    }

    private void recordDebt(String fromUserId, String toUserId, double amount) {
        ConcurrentHashMap<String, Double> userBalances = getOrCreateBalancesForUser(fromUserId);
        double currentBalance = userBalances.getOrDefault(toUserId, 0.0);
        double updatedBalance = currentBalance + amount;
        userBalances.put(toUserId, updatedBalance);
    }

    private ConcurrentHashMap<String, Double> getOrCreateBalancesForUser(String userId) {
        ConcurrentHashMap<String, Double> userBalances = balances.get(userId);

        if (userBalances == null) {
            userBalances = new ConcurrentHashMap<>();
            balances.put(userId, userBalances);
        }

        return userBalances;
    }
}
