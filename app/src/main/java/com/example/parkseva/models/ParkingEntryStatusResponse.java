package com.example.parkseva.models;

public class ParkingEntryStatusResponse {
    private String _id;
    private String status;
    private String entryTime;
    private String vehicleNumber;
    private VehicleInfo vehicleId;
    private SlotInfo slotId;

    public String getId() {
        return _id;
    }

    public String getStatus() {
        return status;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public VehicleInfo getVehicleId() {
        return vehicleId;
    }

    public SlotInfo getSlotId() {
        return slotId;
    }

    public String getResolvedVehicleNumber() {
        if (vehicleNumber != null && !vehicleNumber.trim().isEmpty()) {
            return vehicleNumber;
        }
        if (vehicleId != null) {
            return vehicleId.vehicleNumber;
        }
        return "";
    }

    public String getResolvedSlotNumber() {
        if (slotId != null && slotId.slotNumber != null) {
            return slotId.slotNumber;
        }
        return "N/A";
    }

    public static class VehicleInfo {
        private String vehicleNumber;

        public String getVehicleNumber() {
            return vehicleNumber;
        }
    }

    public static class SlotInfo {
        private String slotNumber;

        public String getSlotNumber() {
            return slotNumber;
        }
    }
}
