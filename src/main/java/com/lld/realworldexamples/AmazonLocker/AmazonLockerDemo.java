package com.lld.realworldexamples.AmazonLocker;

import com.lld.realworldexamples.AmazonLocker.entities.Locker;
import com.lld.realworldexamples.AmazonLocker.entities.LockerLocation;
import com.lld.realworldexamples.AmazonLocker.entities.Package;
import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;
import com.lld.realworldexamples.AmazonLocker.exceptions.InvalidCodeException;
import com.lld.realworldexamples.AmazonLocker.strategies.ConsoleNotificationService;
import com.lld.realworldexamples.AmazonLocker.strategies.SmallestLockerStrategy;

public class AmazonLockerDemo {
    public static void main(String[] args) {
        LockerSystem system = LockerSystem.getInstance();
        system.setAssignmentStrategy(new SmallestLockerStrategy());
        system.setNotificationService(new ConsoleNotificationService());

        // Set up a location with lockers of various sizes
        LockerLocation location = new LockerLocation("LOC-1", "123 Main St, Seattle");
        location.addLocker(new Locker("L1", LockerSize.SMALL));
        location.addLocker(new Locker("L2", LockerSize.SMALL));
        location.addLocker(new Locker("L3", LockerSize.MEDIUM));
        location.addLocker(new Locker("L4", LockerSize.LARGE));
        location.addLocker(new Locker("L5", LockerSize.XL));
        system.addLocation(location);

        // Scenario 1: Deliver a small package and pick it up successfully
        System.out.println("========== SCENARIO 1: Successful Delivery & Pickup ==========");
        Package pkg1 = new Package("PKG-001", "ORD-001", LockerSize.SMALL);
        system.deliverPackage(pkg1, "LOC-1");
        System.out.println("Package PKG-001 delivered. Status: " + pkg1.getStatus());

        String code1 = system.getCodeForPackage("PKG-001");
        Package picked1 = system.pickupPackage("LOC-1", "L1", code1);
        System.out.println("Package picked up. Status: " + picked1.getStatus());

        // Scenario 2: Deliver a medium package, try wrong code, then correct code
        System.out.println("\n========== SCENARIO 2: Wrong Code Then Correct Code ==========");
        Package pkg2 = new Package("PKG-002", "ORD-002", LockerSize.MEDIUM);
        system.deliverPackage(pkg2, "LOC-1");
        System.out.println("Package PKG-002 delivered. Status: " + pkg2.getStatus());

        try {
            system.pickupPackage("LOC-1", "L3", "000000");
        } catch (InvalidCodeException e) {
            System.out.println("Wrong code attempt: " + e.getMessage());
        }

        String code2 = system.getCodeForPackage("PKG-002");
        Package picked2 = system.pickupPackage("LOC-1", "L3", code2);
        System.out.println("Correct code used. Package picked up. Status: " + picked2.getStatus());

        // Scenario 3: Deliver a large package with immediate expiry, then cleanup
        System.out.println("\n========== SCENARIO 3: Expired Package Cleanup ==========");
        Package pkg3 = new Package("PKG-003", "ORD-003", LockerSize.LARGE);
        system.deliverPackageWithCustomExpiry(pkg3, "LOC-1", 0);
        System.out.println("Package PKG-003 delivered with immediate expiry.");

        System.out.println("Running cleanup...");
        system.cleanupExpiredPackages();
        System.out.println("Package PKG-003 status after cleanup: " + pkg3.getStatus());
    }
}
