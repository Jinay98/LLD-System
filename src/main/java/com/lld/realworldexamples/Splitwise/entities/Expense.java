package com.lld.realworldexamples.Splitwise.entities;

import com.lld.realworldexamples.Splitwise.entities.split.Split;
import com.lld.realworldexamples.Splitwise.enums.SplitType;

import java.time.LocalDateTime;
import java.util.List;

public class Expense {
    private final String id;
    private final double amount;
    private final String description;
    private final String paidByUserId;
    private final SplitType splitType;
    private final List<Split> splits;
    private final String groupId;
    private final LocalDateTime createdAt;

    public Expense(String id, double amount, String description,
                   String paidByUserId, SplitType splitType,
                   List<Split> splits, String groupId) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.paidByUserId = paidByUserId;
        this.splitType = splitType;
        this.splits = List.copyOf(splits);
        this.groupId = groupId;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getPaidByUserId() {
        return paidByUserId;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public String getGroupId() {
        return groupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
