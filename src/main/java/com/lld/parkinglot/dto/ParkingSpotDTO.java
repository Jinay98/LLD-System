package com.lld.parkinglot.dto;

/**
 * DTO for parking spot response/request
 */
public class ParkingSpotDTO {
    private int spotNumber;
    private String level;
    private boolean isOccupied;

    public ParkingSpotDTO(int spotNumber, String level, boolean isOccupied) {
        this.spotNumber = spotNumber;
        this.level = level;
        this.isOccupied = isOccupied;
    }

    // Getters and setters
    public int getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(int spotNumber) {
        this.spotNumber = spotNumber;
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

    @Override
    public String toString() {
        return "ParkingSpotDTO{" +
                "spotNumber=" + spotNumber +
                ", level='" + level + '\'' +
                ", isOccupied=" + isOccupied +
                '}';
    }
}
