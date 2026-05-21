package com.lld.parkinglot.services;

import com.lld.parkinglot.dto.ParkingSpotDTO;

/**
 * Service interface for parking lot business logic
 */
public interface ParkingLotService {
    ParkingSpotDTO findAvailableSpot();

    boolean parkVehicle(String vehicleNumber, String vehicleType);

    boolean unparkVehicle(String vehicleNumber);

    void displayAvailableSpots();
}
