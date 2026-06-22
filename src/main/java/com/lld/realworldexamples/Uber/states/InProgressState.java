package com.lld.realworldexamples.Uber.states;

import com.lld.realworldexamples.Uber.enums.TripStatus;
import com.lld.realworldexamples.Uber.models.Driver;
import com.lld.realworldexamples.Uber.models.Trip;

public class InProgressState implements TripState {
    @Override
    public TripStatus getStatus() {
        return TripStatus.IN_PROGRESS;
    }

    @Override
    public void assign(Trip trip, Driver driver) {
        System.out.println("Cannot assign a new driver while trip is in progress.");
    }

    @Override
    public void start(Trip trip) {
        System.out.println("Trip is already in progress.");
    }

    @Override
    public void end(Trip trip) {
        trip.setState(new CompletedState());
    }
}
