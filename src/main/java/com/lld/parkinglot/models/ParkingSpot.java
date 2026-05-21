package com.lld.parkinglot.models;

/**
 * Model for parking spot entity
 */
public class ParkingSpot {
    private int spotId;
    private String level;
    private boolean isOccupied;
    private Vehicle parkedVehicle;

    public ParkingSpot(int spotId, String level) {
        this.spotId = spotId;
        this.level = level;
        this.isOccupied = false;
        this.parkedVehicle = null;
    }

    // Getters and setters
    public int getSpotId() {
        return spotId;
    }

    public void setSpotId(int spotId) {
        this.spotId = spotId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public void setParkedVehicle(Vehicle parkedVehicle) {
        this.parkedVehicle = parkedVehicle;
    }
}
