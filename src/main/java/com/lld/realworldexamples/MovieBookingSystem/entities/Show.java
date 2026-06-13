package com.lld.realworldexamples.MovieBookingSystem.entities;

import com.lld.realworldexamples.MovieBookingSystem.enums.SeatType;
import com.lld.realworldexamples.MovieBookingSystem.strategies.PricingStrategy;

import java.time.LocalDateTime;
import java.util.Map;

public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final Map<SeatType, Double> seatPrices;
    private final PricingStrategy pricingStrategy;
    // Dedicated monitor for serializing seat lock/unlock/confirm on this show.
    private final Object lock = new Object();

    public Show(String id, Movie movie, Screen screen, LocalDateTime startTime,
                Map<SeatType, Double> seatPrices, PricingStrategy pricingStrategy) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seatPrices = seatPrices;
        this.pricingStrategy = pricingStrategy;
    }

    public String getId() { return id; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public LocalDateTime getStartTime() { return startTime; }
    public Map<SeatType, Double> getSeatPrices() { return seatPrices; }
    public PricingStrategy getPricingStrategy() { return pricingStrategy; }
    public Object getLock() { return lock; }
}
