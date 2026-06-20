package com.lld.realworldexamples.AmazonLocker.strategies;

import com.lld.realworldexamples.AmazonLocker.entities.Locker;
import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;
import com.lld.realworldexamples.AmazonLocker.exceptions.NoAvailableLockerException;

import java.util.List;

public class SmallestLockerStrategy implements LockerAssignmentStrategy {
    @Override
    public Locker assignLocker(LockerSize requiredSize, List<Locker> availableLockers) {
        LockerSize[] sizes = LockerSize.values();

        // Start from the required size and try increasingly larger sizes
        for (int i = requiredSize.ordinal(); i < sizes.length; i++) {
            LockerSize targetSize = sizes[i];
            for (Locker locker : availableLockers) {
                if (locker.getSize() == targetSize && locker.isAvailable()) {
                    return locker;
                }
            }
        }

        throw new NoAvailableLockerException(
                "No available locker for size " + requiredSize + " or larger"
        );
    }
}
