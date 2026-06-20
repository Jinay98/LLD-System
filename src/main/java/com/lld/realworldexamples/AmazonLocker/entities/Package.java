package com.lld.realworldexamples.AmazonLocker.entities;

import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;
import com.lld.realworldexamples.AmazonLocker.enums.PackageStatus;

import com.lld.realworldexamples.AmazonLocker.entities.Package;

public class Package {
    private final String id;
    private final String orderId;
    private final LockerSize lockerSize;
    private PackageStatus status;

    public Package(String id, String orderId, LockerSize lockerSize) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Package ID cannot be null or empty");
        }
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        this.id = id;
        this.orderId = orderId;
        this.lockerSize = lockerSize;
        this.status = PackageStatus.CREATED;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public LockerSize getLockerSize() {
        return lockerSize;
    }

    public PackageStatus getStatus() {
        return status;
    }

    public void setStatus(PackageStatus status) {
        this.status = status;
    }
}
