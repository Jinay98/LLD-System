package com.lld.realworldexamples.Uber.observers;

import com.lld.realworldexamples.Uber.models.Trip;

public interface TripObserver {
    void onUpdate(Trip trip);
}
