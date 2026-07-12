package com.evcharging;

import java.io.Serializable;

public class ChargingStation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String stationId;
    private final String name;
    private final String location;
    private final int totalSlots;
    private int availableSlots;
    private final double ratePerKWh;

    public ChargingStation(String stationId, String name, String location, int totalSlots, double ratePerKWh) {
        this.stationId = stationId;
        this.name = name;
        this.location = location;
        this.totalSlots = totalSlots;
        this.availableSlots = totalSlots;
        this.ratePerKWh = ratePerKWh;
    }

    public String getStationId() { return stationId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getTotalSlots() { return totalSlots; }
    public int getAvailableSlots() { return availableSlots; }
    public double getRatePerKWh() { return ratePerKWh; }

    public boolean hasAvailableSlot() {
        return availableSlots > 0;
    }

    public void occupySlot() {
        if (availableSlots > 0) availableSlots--;
    }

    public void freeSlot() {
        if (availableSlots < totalSlots) availableSlots++;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-18s @ %-15s | Slots: %d/%d free | Rate: Rs.%.2f/kWh",
                stationId, name, location, availableSlots, totalSlots, ratePerKWh);
    }
}
