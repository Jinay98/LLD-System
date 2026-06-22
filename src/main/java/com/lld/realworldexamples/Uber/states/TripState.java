package com.lld.realworldexamples.Uber.states;

import com.lld.realworldexamples.Uber.enums.TripStatus;
import com.lld.realworldexamples.Uber.models.Driver;
import com.lld.realworldexamples.Uber.models.Trip;

public interface TripState {
    TripStatus getStatus();

    void assign(Trip trip, Driver driver);

    void start(Trip trip);

    void end(Trip trip);
}
