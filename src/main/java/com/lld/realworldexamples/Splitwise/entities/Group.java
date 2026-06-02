package com.lld.realworldexamples.Splitwise.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {
    private final String id;
    private final String name;
    private final List<String> memberIds;
    private final List<String> expenseIds;

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.memberIds = new ArrayList<>();
        this.expenseIds = new ArrayList<>();
    }

    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void addExpense(String expenseId) {
        expenseIds.add(expenseId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMemberIds() {
        return Collections.unmodifiableList(memberIds);
    }

    public List<String> getExpenseIds() {
        return Collections.unmodifiableList(expenseIds);
    }
}
