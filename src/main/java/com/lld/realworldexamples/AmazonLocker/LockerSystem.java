package com.lld.realworldexamples.AmazonLocker;

import com.lld.realworldexamples.AmazonLocker.entities.Locker;
import com.lld.realworldexamples.AmazonLocker.entities.LockerCode;
import com.lld.realworldexamples.AmazonLocker.entities.LockerLocation;
import com.lld.realworldexamples.AmazonLocker.entities.Package;
import com.lld.realworldexamples.AmazonLocker.enums.LockerStatus;
import com.lld.realworldexamples.AmazonLocker.enums.PackageStatus;
import com.lld.realworldexamples.AmazonLocker.exceptions.InvalidCodeException;
import com.lld.realworldexamples.AmazonLocker.exceptions.PackageAlreadyPickedUpException;
import com.lld.realworldexamples.AmazonLocker.exceptions.PackageExpiredException;
import com.lld.realworldexamples.AmazonLocker.strategies.LockerAssignmentStrategy;
import com.lld.realworldexamples.AmazonLocker.strategies.NotificationService;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class LockerSystem {
    private static final Object lock = new Object();
    private static volatile LockerSystem instance;
    private final ConcurrentHashMap<String, LockerLocation> locations;
    // Maps package ID -> locker ID for quick lookup
    private final ConcurrentHashMap<String, String> packageLockerMap;
    // Maps package ID -> location ID for quick lookup
    private final ConcurrentHashMap<String, String> packageLocationMap;
    // Maps package ID -> code string for demo convenience
    private final ConcurrentHashMap<String, String> packageCodeMap;
    private LockerAssignmentStrategy assignmentStrategy;
    private NotificationService notificationService;

    private LockerSystem() {
        this.locations = new ConcurrentHashMap<>();
        this.packageLockerMap = new ConcurrentHashMap<>();
        this.packageLocationMap = new ConcurrentHashMap<>();
        this.packageCodeMap = new ConcurrentHashMap<>();
    }

    public static LockerSystem getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new LockerSystem();
                }
            }
        }
        return instance;
    }

    public void setAssignmentStrategy(LockerAssignmentStrategy strategy) {
        this.assignmentStrategy = strategy;
    }

    public void setNotificationService(NotificationService service) {
        this.notificationService = service;
    }

    public void addLocation(LockerLocation location) {
        locations.put(location.getId(), location);
    }

    public void deliverPackage(Package pkg, String locationId) {
        deliverPackageWithCustomExpiry(pkg, locationId, 3 * 24 * 60 * 60);
    }

    public void deliverPackageWithCustomExpiry(Package pkg, String locationId, long expirySeconds) {
        LockerLocation location = locations.get(locationId);
        if (location == null) {
            throw new IllegalArgumentException("Location " + locationId + " not found");
        }

        // Gather all available lockers at this location
        List<Locker> availableLockers = location.getAllLockers();

        // Delegate locker selection to the strategy
        Locker locker = assignmentStrategy.assignLocker(pkg.getLockerSize(), availableLockers);

        // Generate a 6-digit pickup code
        String code = generateCode();
        long expirationTime = System.currentTimeMillis() + (expirySeconds * 1000);
        LockerCode lockerCode = new LockerCode(code, pkg.getId(), expirationTime);

        // Assign the package to the selected locker
        locker.assignPackage(pkg, lockerCode);
        pkg.setStatus(PackageStatus.DELIVERED);

        // Track the mapping for pickup and cleanup
        packageLockerMap.put(pkg.getId(), locker.getId());
        packageLocationMap.put(pkg.getId(), locationId);
        packageCodeMap.put(pkg.getId(), code);

        // Notify the customer
        notificationService.notifyCustomer(
                pkg.getOrderId(), code, location.getAddress(), expirationTime
        );
    }

    public Package pickupPackage(String locationId, String lockerId, String code) {
        LockerLocation location = locations.get(locationId);
        if (location == null) {
            throw new IllegalArgumentException("Location " + locationId + " not found");
        }

        Locker locker = location.getLocker(lockerId);

        // Synchronized access to check code and release package atomically
        synchronized (locker) {
            if (locker.getStatus() != LockerStatus.OCCUPIED) {
                throw new IllegalStateException("Locker " + lockerId + " is not occupied");
            }

            LockerCode lockerCode = locker.getCurrentCode();
            Package pkg = locker.getCurrentPackage();

            // Check if already picked up
            if (pkg.getStatus() == PackageStatus.PICKED_UP) {
                throw new PackageAlreadyPickedUpException(
                        "Package " + pkg.getId() + " has already been picked up"
                );
            }

            // Check expiry before validating code
            if (lockerCode.isExpired()) {
                throw new PackageExpiredException(
                        "Package " + pkg.getId() + " has expired. Please contact support."
                );
            }

            // Validate the pickup code
            if (!lockerCode.getCode().equals(code)) {
                throw new InvalidCodeException("Invalid pickup code for locker " + lockerId);
            }

            // Release the package
            locker.releasePackage();
            pkg.setStatus(PackageStatus.PICKED_UP);

            // Clean up tracking maps
            packageLockerMap.remove(pkg.getId());
            packageLocationMap.remove(pkg.getId());
            packageCodeMap.remove(pkg.getId());

            return pkg;
        }
    }

    public void cleanupExpiredPackages() {
        for (LockerLocation location : locations.values()) {
            for (Locker locker : location.getAllLockers()) {
                synchronized (locker) {
                    if (locker.getStatus() == LockerStatus.OCCUPIED) {
                        LockerCode code = locker.getCurrentCode();
                        if (code != null && code.isExpired()) {
                            Package pkg = locker.releasePackage();
                            pkg.setStatus(PackageStatus.RETURNED);

                            packageLockerMap.remove(pkg.getId());
                            packageLocationMap.remove(pkg.getId());
                            packageCodeMap.remove(pkg.getId());

                            System.out.println("[CLEANUP] Package " + pkg.getId()
                                    + " expired. Returning package and freeing locker "
                                    + locker.getId() + ".");
                        }
                    }
                }
            }
        }
    }

    public String getCodeForPackage(String packageId) {
        return packageCodeMap.get(packageId);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
