package com.lld.realworldexamples.MovieBookingSystem.strategies;

import com.lld.realworldexamples.MovieBookingSystem.entities.Seat;
import com.lld.realworldexamples.MovieBookingSystem.enums.SeatType;

import java.util.List;
import java.util.Map;

public class WeekdayPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(List<Seat> seats, Map<SeatType, Double> seatPrices) {
        return seats.stream().mapToDouble(seat -> seatPrices.get(seat.getType())).sum();
    }
}

