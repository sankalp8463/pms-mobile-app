package com.example.parkseva.models;

public class ParkingSlot {
    private String _id;
    private String slotNumber;
    private String status;
    private String vehicleType;
    private double hourlyRate;

    public ParkingSlot() {}

    public ParkingSlot(String slotNumber, String status, String vehicleType, double hourlyRate) {
        this.slotNumber = slotNumber;
        this.status = status;
        this.vehicleType = vehicleType;
        this.hourlyRate = hourlyRate;
    }

    // Getters and Setters
    public String getId() { return _id; }
    public void setId(String id) { this._id = id; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
}