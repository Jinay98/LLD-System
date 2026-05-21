package com.lld.parkinglot.repository;

import com.lld.parkinglot.models.ParkingSpot;

import java.util.List;

/**
 * Repository interface for parking spot data access
 */
public interface ParkingSpotRepository {
    ParkingSpot save(ParkingSpot spot);

    ParkingSpot findById(int spotId);

    List<ParkingSpot> findAll();

    void update(ParkingSpot spot);

    void delete(int spotId);
}
