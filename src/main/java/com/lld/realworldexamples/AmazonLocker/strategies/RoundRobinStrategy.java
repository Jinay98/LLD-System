package com.lld.realworldexamples.AmazonLocker.strategies;

import com.lld.realworldexamples.AmazonLocker.entities.Locker;
import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;
import com.lld.realworldexamples.AmazonLocker.exceptions.NoAvailableLockerException;

import java.util.ArrayList;
import java.util.List;

public class RoundRobinStrategy implements LockerAssignmentStrategy {
    private int lastAssignedIndex = 0;

    @Override
    public synchronized Locker assignLocker(LockerSize requiredSize, List<Locker> availableLockers) {
        LockerSize[] sizes = LockerSize.values();

        for (int i = requiredSize.ordinal(); i < sizes.length; i++) {
            LockerSize targetSize = sizes[i];
            List<Locker> matching = new ArrayList<>();
            for (Locker locker : availableLockers) {
                if (locker.getSize() == targetSize && locker.isAvailable()) {
                    matching.add(locker);
                }
            }
            if (!matching.isEmpty()) {
                lastAssignedIndex = (lastAssignedIndex + 1) % matching.size();
                return matching.get(lastAssignedIndex);
            }
        }

        throw new NoAvailableLockerException(
                "No available locker for size " + requiredSize + " or larger"
        );
    }
}
