package com.lld.realworldexamples.AmazonLocker.entities;

import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;
import com.lld.realworldexamples.AmazonLocker.enums.LockerStatus;

public class Locker {
    private final String id;
    private final LockerSize size;
    private LockerStatus status;
    private Package currentPackage;
    private LockerCode currentCode;

    public Locker(String id, LockerSize size) {
        this.id = id;
        this.size = size;
        this.status = LockerStatus.AVAILABLE;
        this.currentPackage = null;
        this.currentCode = null;
    }

    // Synchronized to prevent two threads from assigning to the same locker
    public synchronized void assignPackage(Package pkg, LockerCode code) {
        if (status != LockerStatus.AVAILABLE) {
            throw new IllegalStateException("Locker " + id + " is not available");
        }
        this.currentPackage = pkg;
        this.currentCode = code;
        this.status = LockerStatus.OCCUPIED;
    }

    // Synchronized to prevent race between pickup and cleanup
    public synchronized Package releasePackage() {
        Package pkg = this.currentPackage;
        this.currentPackage = null;
        this.currentCode = null;
        this.status = LockerStatus.AVAILABLE;
        return pkg;
    }

    public boolean isAvailable() {
        return status == LockerStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public LockerSize getSize() {
        return size;
    }

    public LockerStatus getStatus() {
        return status;
    }

    public Package getCurrentPackage() {
        return currentPackage;
    }

    public LockerCode getCurrentCode() {
        return currentCode;
    }
}
