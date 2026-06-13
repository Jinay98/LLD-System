package com.lld.realworldexamples.MovieBookingSystem.entities;

import java.util.List;
import java.util.UUID;

public class Booking {
    private final String id;
    private final User user;
    private final Show show;
    private final List<Seat> seats;
    private final double totalAmount;
    private final Payment payment;

    // All fields are required and set in one place, so a plain constructor is enough.
    public Booking(User user, Show show, List<Seat> seats, double totalAmount, Payment payment) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.payment = payment;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Show getShow() {
        return show;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Payment getPayment() {
        return payment;
    }
}
