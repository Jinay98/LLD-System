package com.lld.realworldexamples.MovieBookingSystem.strategies;

import com.lld.realworldexamples.MovieBookingSystem.entities.Seat;
import com.lld.realworldexamples.MovieBookingSystem.enums.SeatType;

import java.util.List;
import java.util.Map;

public interface PricingStrategy {
    double calculatePrice(List<Seat> seats, Map<SeatType, Double> seatPrices);
}
