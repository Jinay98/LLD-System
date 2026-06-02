package com.lld.realworldexamples.Splitwise.services;


import com.lld.realworldexamples.Splitwise.entities.BalanceSheet;
import com.lld.realworldexamples.Splitwise.entities.Expense;
import com.lld.realworldexamples.Splitwise.entities.Group;
import com.lld.realworldexamples.Splitwise.entities.User;
import com.lld.realworldexamples.Splitwise.entities.split.Split;
import com.lld.realworldexamples.Splitwise.enums.SplitType;
import com.lld.realworldexamples.Splitwise.exceptions.GroupNotFoundException;
import com.lld.realworldexamples.Splitwise.exceptions.UserNotFoundException;
import com.lld.realworldexamples.Splitwise.observers.ExpenseObserver;
import com.lld.realworldexamples.Splitwise.strategies.EqualSplitStrategy;
import com.lld.realworldexamples.Splitwise.strategies.ExactSplitStrategy;
import com.lld.realworldexamples.Splitwise.strategies.PercentageSplitStrategy;
import com.lld.realworldexamples.Splitwise.strategies.SplitStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplitwiseService {
    private static final Object lock = new Object();
    private static volatile SplitwiseService instance;
    private final ConcurrentHashMap<String, User> users;
    private final ConcurrentHashMap<String, Group> groups;
    private final ConcurrentHashMap<String, Expense> expenses;
    private final Map<SplitType, SplitStrategy> strategies;
    private final BalanceSheet balanceSheet;
    // Thread-safe list for observers, allows concurrent iteration and modification
    private final CopyOnWriteArrayList<ExpenseObserver> observers;

    private SplitwiseService() {
        this.users = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.expenses = new ConcurrentHashMap<>();
        this.balanceSheet = new BalanceSheet();
        this.observers = new CopyOnWriteArrayList<>();

        // Register all split strategies
        this.strategies = new HashMap<>();
        this.strategies.put(SplitType.EQUAL, new EqualSplitStrategy());
        this.strategies.put(SplitType.EXACT, new ExactSplitStrategy());
        this.strategies.put(SplitType.PERCENTAGE, new PercentageSplitStrategy());
    }

    public static SplitwiseService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SplitwiseService();
                }
            }
        }
        return instance;
    }

    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public Group createGroup(String id, String name) {
        Group group = new Group(id, name);
        groups.put(id, group);
        return group;
    }

    public void addMemberToGroup(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new GroupNotFoundException("Group not found: " + groupId);
        }
        if (!users.containsKey(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        group.addMember(userId);
    }

    public Expense addExpense(String id, double amount, String description,
                              String paidByUserId, SplitType splitType,
                              List<Split> splits, String groupId) {
        // 1. Validate payer exists
        if (!users.containsKey(paidByUserId)) {
            throw new UserNotFoundException("User not found: " + paidByUserId);
        }

        // 2. Look up and apply the strategy
        SplitStrategy strategy = strategies.get(splitType);
        strategy.validate(splits, amount);
        strategy.calculateSplits(splits, amount);

        // 3. Create the expense
        Expense expense = new Expense(id, amount, description, paidByUserId,
                splitType, splits, groupId);
        expenses.put(id, expense);

        // 4. Associate with group if applicable
        if (groupId != null) {
            Group group = groups.get(groupId);
            if (group == null) {
                throw new GroupNotFoundException("Group not found: " + groupId);
            }
            group.addExpense(id);
        }

        // 5. Update balances - each participant owes the payer their share
        for (Split split : expense.getSplits()) {
            if (!split.getUserId().equals(paidByUserId)) {
                balanceSheet.updateBalance(split.getUserId(), paidByUserId, split.getAmount());
            }
        }

        // 6. Notify observers
        for (ExpenseObserver observer : observers) {
            observer.onExpenseAdded(expense);
        }

        return expense;
    }

    public void settleUp(String fromUserId, String toUserId, double amount) {
        if (!users.containsKey(fromUserId)) {
            throw new UserNotFoundException("User not found: " + fromUserId);
        }
        if (!users.containsKey(toUserId)) {
            throw new UserNotFoundException("User not found: " + toUserId);
        }

        balanceSheet.settleUp(fromUserId, toUserId, amount);

        for (ExpenseObserver observer : observers) {
            observer.onSettlement(fromUserId, toUserId, amount);
        }
    }

    public double getBalance(String userId1, String userId2) {
        return balanceSheet.getBalance(userId1, userId2);
    }

    public Map<String, Double> getBalancesForUser(String userId) {
        return balanceSheet.getBalancesForUser(userId);
    }

    public void addObserver(ExpenseObserver observer) {
        observers.add(observer);
    }
}
