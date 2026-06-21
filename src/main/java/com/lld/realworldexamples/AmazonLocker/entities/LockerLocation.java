package com.lld.realworldexamples.AmazonLocker.entities;

import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LockerLocation {
    private final String id;
    private final String address;
    private final ConcurrentHashMap<String, Locker> lockers;

    public LockerLocation(String id, String address) {
        this.id = id;
        this.address = address;
        this.lockers = new ConcurrentHashMap<>();
    }

    public void addLocker(Locker locker) {
        lockers.put(locker.getId(), locker);
    }

    public List<Locker> getAvailableLockersBySize(LockerSize size) {
        List<Locker> result = new ArrayList<>();
        for (Locker locker : lockers.values()) {
            if (locker.isAvailable() && locker.getSize() == size) {
                result.add(locker);
            }
        }
        return result;
    }

    public List<Locker> getAllLockers() {
        return new ArrayList<>(lockers.values());
    }

    public Locker getLocker(String lockerId) {
        Locker locker = lockers.get(lockerId);
        if (locker == null) {
            throw new IllegalArgumentException("Locker " + lockerId + " not found at location " + id);
        }
        return locker;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }
}
