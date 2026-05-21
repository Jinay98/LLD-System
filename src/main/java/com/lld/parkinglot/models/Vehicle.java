package com.lld.parkinglot.models;

/**
 * Model for vehicle entity
 */
public class Vehicle {
    private String licenseNumber;
    private String type;

    public Vehicle(String licenseNumber, String type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
