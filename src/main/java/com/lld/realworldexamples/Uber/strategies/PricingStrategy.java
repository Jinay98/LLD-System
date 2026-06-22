package com.lld.realworldexamples.Uber.strategies;

import com.lld.realworldexamples.Uber.enums.RideType;
import com.lld.realworldexamples.Uber.models.Location;

public interface PricingStrategy {
    double calculateFare(Location pickup, Location dropoff, RideType rideType);
}
