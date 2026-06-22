package com.lld.realworldexamples.Uber.states;

import com.lld.realworldexamples.Uber.enums.TripStatus;
import com.lld.realworldexamples.Uber.models.Driver;
import com.lld.realworldexamples.Uber.models.Trip;

public class CompletedState implements TripState {
    @Override
    public TripStatus getStatus() { return TripStatus.COMPLETED; }

    @Override
    public void assign(Trip trip, Driver driver) {
        System.out.println("Cannot assign a driver to a completed trip.");
    }

    @Override
    public void start(Trip trip) {
        System.out.println("Cannot start a completed trip.");
    }

    @Override
    public void end(Trip trip) {
        System.out.println("Trip is already completed.");
    }
}
