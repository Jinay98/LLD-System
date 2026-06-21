package com.lld.realworldexamples.AmazonLocker.strategies;

import com.lld.realworldexamples.AmazonLocker.entities.Locker;
import com.lld.realworldexamples.AmazonLocker.enums.LockerSize;

import java.util.List;

public interface LockerAssignmentStrategy {
    Locker assignLocker(LockerSize requiredSize, List<Locker> availableLockers);
}
