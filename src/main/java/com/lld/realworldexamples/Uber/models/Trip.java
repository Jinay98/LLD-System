package com.lld.realworldexamples.Uber.models;

import com.lld.realworldexamples.Uber.enums.TripStatus;
import com.lld.realworldexamples.Uber.observers.TripObserver;
import com.lld.realworldexamples.Uber.states.RequestedState;
import com.lld.realworldexamples.Uber.states.TripState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Trip {
    private final String id;
    private final Rider rider;
    private final Location pickupLocation;
    private final Location dropoffLocation;
    private final double fare;
    private final List<TripObserver> observers = new ArrayList<>();
    private Driver driver;
    private TripState currentState;

    private Trip(TripBuilder builder) {
        this.id = builder.id;
        this.rider = builder.rider;
        this.pickupLocation = builder.pickupLocation;
        this.dropoffLocation = builder.dropoffLocation;
        this.fare = builder.fare;
        this.currentState = new RequestedState(); // Initial state
        addObserver(this.rider);
    }

    public void addObserver(TripObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        observers.forEach(o -> o.onUpdate(this));
    }

    public void assignDriver(Driver driver) {
        TripState previousState = currentState;
        currentState.assign(this, driver);
        if (currentState != previousState) { // proceed only if the assignment was accepted
            addObserver(driver);
            notifyObservers();
        }
    }

    public void startTrip() {
        TripState previousState = currentState;
        currentState.start(this);
        if (currentState != previousState) {
            notifyObservers();
        }
    }

    public void endTrip() {
        TripState previousState = currentState;
        currentState.end(this);
        if (currentState != previousState) {
            notifyObservers();
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public Rider getRider() {
        return rider;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public Location getDropoffLocation() {
        return dropoffLocation;
    }

    public double getFare() {
        return fare;
    }

    public TripStatus getStatus() {
        return currentState.getStatus();
    }

    // These setters are called by the State objects to drive transitions
    public void setState(TripState state) {
        this.currentState = state;
    }

    @Override
    public String toString() {
        return "Trip [id=" + id + ", status=" + getStatus() + ", fare=$" + String.format("%.2f", fare) + "]";
    }

    // --- Builder Pattern ---
    public static class TripBuilder {
        private final String id;
        private Rider rider;
        private Location pickupLocation;
        private Location dropoffLocation;
        private double fare;

        public TripBuilder() {
            this.id = UUID.randomUUID().toString();
        }

        public TripBuilder withRider(Rider rider) {
            this.rider = rider;
            return this;
        }

        public TripBuilder withPickupLocation(Location pickupLocation) {
            this.pickupLocation = pickupLocation;
            return this;
        }

        public TripBuilder withDropoffLocation(Location dropoffLocation) {
            this.dropoffLocation = dropoffLocation;
            return this;
        }

        public TripBuilder withFare(double fare) {
            this.fare = fare;
            return this;
        }

        public Trip build() {
            // Basic validation
            if (rider == null || pickupLocation == null || dropoffLocation == null) {
                throw new IllegalStateException("Rider, pickup, and dropoff locations are required to build a trip.");
            }
            return new Trip(this);
        }
    }
}
