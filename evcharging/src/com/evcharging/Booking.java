package com.evcharging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String bookingId;
    private final String userName;
    private final String vehicleNumber;
    private final String stationId;
    private final double batteryCapacityKWh;
    private final double startBatteryPercent;
    private double currentBatteryPercent;
    private final double targetBatteryPercent;
    private BookingStatus status;
    private final LocalDateTime bookingTime;
    private LocalDateTime chargingStartTime;
    private LocalDateTime completionTime;
    private double cost;

    public Booking(String bookingId, String userName, String vehicleNumber, String stationId,
                    double batteryCapacityKWh, double startBatteryPercent, double targetBatteryPercent,
                    BookingStatus status) {
        this.bookingId = bookingId;
        this.userName = userName;
        this.vehicleNumber = vehicleNumber;
        this.stationId = stationId;
        this.batteryCapacityKWh = batteryCapacityKWh;
        this.startBatteryPercent = startBatteryPercent;
        this.currentBatteryPercent = startBatteryPercent;
        this.targetBatteryPercent = targetBatteryPercent;
        this.status = status;
        this.bookingTime = LocalDateTime.now();
        if (status == BookingStatus.CHARGING) {
            this.chargingStartTime = LocalDateTime.now();
        }
    }

    public String getBookingId() { return bookingId; }
    public String getUserName() { return userName; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getStationId() { return stationId; }
    public double getBatteryCapacityKWh() { return batteryCapacityKWh; }
    public double getStartBatteryPercent() { return startBatteryPercent; }
    public double getCurrentBatteryPercent() { return currentBatteryPercent; }
    public double getTargetBatteryPercent() { return targetBatteryPercent; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public LocalDateTime getChargingStartTime() { return chargingStartTime; }
    public LocalDateTime getCompletionTime() { return completionTime; }
    public double getCost() { return cost; }

    public void setCurrentBatteryPercent(double p) { this.currentBatteryPercent = p; }
    public void setCost(double cost) { this.cost = cost; }

    public void markChargingStarted() {
        this.status = BookingStatus.CHARGING;
        this.chargingStartTime = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = BookingStatus.COMPLETED;
        this.completionTime = LocalDateTime.now();
    }

    public String getBookingTimeFormatted() { return bookingTime.format(FMT); }
    public String getCompletionTimeFormatted() { return completionTime == null ? "-" : completionTime.format(FMT); }

    @Override
    public String toString() {
        return String.format("%s | %-10s | %-10s | Station:%s | %.0f%% -> %.0f%% (target %.0f%%) | %s",
                bookingId, userName, vehicleNumber, stationId, startBatteryPercent, currentBatteryPercent,
                targetBatteryPercent, status);
    }
}
