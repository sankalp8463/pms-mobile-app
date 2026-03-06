package com.example.parkseva.models;

public class BookingRequest {
    private String vehicleNumber;
    private String vehicleType;
    private String slotId;
    private int hours;

    public BookingRequest(String vehicleNumber, String vehicleType, String slotId, int hours) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.slotId = slotId;
        this.hours = hours;
    }

    // Getters and Setters
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
}