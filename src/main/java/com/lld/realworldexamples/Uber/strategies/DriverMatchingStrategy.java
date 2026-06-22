package com.lld.realworldexamples.Uber.strategies;

import com.lld.realworldexamples.Uber.enums.RideType;
import com.lld.realworldexamples.Uber.models.Driver;
import com.lld.realworldexamples.Uber.models.Location;

import java.util.List;

public interface DriverMatchingStrategy {
    List<Driver> findDrivers(List<Driver> allDrivers, Location pickupLocation,
                             RideType rideType);
}
