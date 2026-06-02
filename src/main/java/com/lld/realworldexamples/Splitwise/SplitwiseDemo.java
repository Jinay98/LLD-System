package com.lld.realworldexamples.Splitwise;

import com.lld.realworldexamples.Splitwise.entities.Group;
import com.lld.realworldexamples.Splitwise.entities.User;
import com.lld.realworldexamples.Splitwise.entities.split.EqualSplit;
import com.lld.realworldexamples.Splitwise.entities.split.ExactSplit;
import com.lld.realworldexamples.Splitwise.entities.split.PercentageSplit;
import com.lld.realworldexamples.Splitwise.entities.split.Split;
import com.lld.realworldexamples.Splitwise.enums.SplitType;
import com.lld.realworldexamples.Splitwise.observers.EmailNotificationObserver;
import com.lld.realworldexamples.Splitwise.services.SplitwiseService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SplitwiseDemo {
    public static void main(String[] args) {
        SplitwiseService service = SplitwiseService.getInstance();

        // Register observer
        service.addObserver(new EmailNotificationObserver());

        // Create users
        User alice = new User("u1", "Alice", "alice@email.com", "1111111111");
        User bob = new User("u2", "Bob", "bob@email.com", "2222222222");
        User charlie = new User("u3", "Charlie", "charlie@email.com", "3333333333");

        service.addUser(alice);
        service.addUser(bob);
        service.addUser(charlie);

        // Create a group
        Group tripGroup = service.createGroup("g1", "Trip to Goa");
        service.addMemberToGroup("g1", "u1");
        service.addMemberToGroup("g1", "u2");
        service.addMemberToGroup("g1", "u3");

        // ===== SCENARIO 1: Equal Split =====
        System.out.println("===== SCENARIO 1: Equal Split =====");
        List<Split> equalSplits = Arrays.asList(
                new EqualSplit("u1"),
                new EqualSplit("u2"),
                new EqualSplit("u3")
        );
        service.addExpense("e1", 300.00, "Dinner", "u1",
                SplitType.EQUAL, equalSplits, "g1");

        System.out.println("\nBalances after dinner:");
        System.out.printf("  Bob owes Alice: $%.2f%n", service.getBalance("u2", "u1"));
        System.out.printf("  Charlie owes Alice: $%.2f%n", service.getBalance("u3", "u1"));

        // ===== SCENARIO 2: Exact Split =====
        System.out.println("\n===== SCENARIO 2: Exact Split =====");
        List<Split> exactSplits = Arrays.asList(
                new ExactSplit("u1", 100.00),
                new ExactSplit("u2", 200.00),
                new ExactSplit("u3", 200.00)
        );
        service.addExpense("e2", 500.00, "Hotel", "u2",
                SplitType.EXACT, exactSplits, "g1");

        System.out.println("\nBalances after hotel:");
        System.out.printf("  Alice owes Bob: $%.2f%n", service.getBalance("u1", "u2"));
        System.out.printf("  Charlie owes Bob: $%.2f%n", service.getBalance("u3", "u2"));

        // ===== SCENARIO 3: Percentage Split =====
        System.out.println("\n===== SCENARIO 3: Percentage Split =====");
        List<Split> percentSplits = Arrays.asList(
                new PercentageSplit("u1", 50),
                new PercentageSplit("u2", 25),
                new PercentageSplit("u3", 25)
        );
        service.addExpense("e3", 400.00, "Activities", "u3",
                SplitType.PERCENTAGE, percentSplits, "g1");

        System.out.println("\nBalances after activities:");
        System.out.printf("  Alice owes Charlie: $%.2f%n", service.getBalance("u1", "u3"));
        System.out.printf("  Bob owes Charlie: $%.2f%n", service.getBalance("u2", "u3"));

        // ===== SCENARIO 4: Settlement =====
        System.out.println("\n===== SCENARIO 4: Settlement =====");
        System.out.println("Bob settles $50 with Alice...");
        service.settleUp("u2", "u1", 50.00);
        System.out.printf("  Bob owes Alice after settlement: $%.2f%n",
                service.getBalance("u2", "u1"));

        // ===== FINAL BALANCES =====
        System.out.println("\n===== FINAL BALANCES =====");
        String[] userIds = {"u1", "u2", "u3"};
        String[] names = {"Alice", "Bob", "Charlie"};

        for (int idx = 0; idx < userIds.length; idx++) {
            Map<String, Double> balances = service.getBalancesForUser(userIds[idx]);
            System.out.println(names[idx] + "'s balances:");
            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                if (Math.abs(entry.getValue()) > 0.01) {
                    String otherName = entry.getKey().equals("u1") ? "Alice" :
                            entry.getKey().equals("u2") ? "Bob" : "Charlie";
                    if (entry.getValue() > 0) {
                        System.out.printf("  Owes %s: $%.2f%n", otherName, entry.getValue());
                    } else {
                        System.out.printf("  Owed by %s: $%.2f%n", otherName, -entry.getValue());
                    }
                }
            }
        }
    }
}
