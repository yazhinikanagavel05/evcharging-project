package com.evcharging;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core service layer: owns stations, bookings, and per-station waiting queues,
 * and implements all business rules (slot allocation, queue promotion, billing).
 */
public class EVChargingSystem {

    private final List<ChargingStation> stations = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final Map<String, Queue<Booking>> waitingQueues = new HashMap<>();

    private final AtomicInteger stationCounter = new AtomicInteger(1);
    private final AtomicInteger bookingCounter = new AtomicInteger(1001);

    public ChargingStation registerStation(String name, String location, int totalSlots, double ratePerKWh) {
        String id = "ST" + stationCounter.getAndIncrement();
        ChargingStation station = new ChargingStation(id, name, location, totalSlots, ratePerKWh);
        stations.add(station);
        waitingQueues.put(id, new LinkedList<>());
        return station;
    }

    public List<ChargingStation> getStations() {
        return stations;
    }

    public Optional<ChargingStation> findStation(String stationId) {
        return stations.stream().filter(s -> s.getStationId().equalsIgnoreCase(stationId)).findFirst();
    }

    /**
     * Books a slot at the given station. If a slot is free, charging starts
     * immediately; otherwise the booking is placed in that station's waiting
     * queue and gets promoted automatically once a slot frees up.
     */
    public Booking bookSlot(String userName, String vehicleNumber, String stationId,
                             double batteryCapacityKWh, double startBatteryPercent, double targetBatteryPercent) {
        ChargingStation station = findStation(stationId)
                .orElseThrow(() -> new NoSuchElementException("No such station: " + stationId));

        String bookingId = "BK" + bookingCounter.getAndIncrement();
        BookingStatus initialStatus = station.hasAvailableSlot() ? BookingStatus.CHARGING : BookingStatus.QUEUED;
        Booking booking = new Booking(bookingId, userName, vehicleNumber, stationId,
                batteryCapacityKWh, startBatteryPercent, targetBatteryPercent, initialStatus);

        if (initialStatus == BookingStatus.CHARGING) {
            station.occupySlot();
        } else {
            waitingQueues.get(stationId).add(booking);
        }

        bookings.add(booking);
        return booking;
    }

    public void updateBatteryPercent(String bookingId, double newPercent) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CHARGING) {
            throw new IllegalStateException("Booking is not currently charging (status: " + booking.getStatus() + ")");
        }
        booking.setCurrentBatteryPercent(newPercent);
    }

    /**
     * Completes charging: computes cost from battery percentage delta,
     * generates an invoice, frees the slot, and promotes the next queued
     * booking (if any) at that station.
     */
    public String completeCharging(String bookingId) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CHARGING) {
            throw new IllegalStateException("Booking is not currently charging (status: " + booking.getStatus() + ")");
        }

        ChargingStation station = findStation(booking.getStationId())
                .orElseThrow(() -> new NoSuchElementException("Station not found"));

        double unitsConsumed = Math.max(0,
                (booking.getCurrentBatteryPercent() - booking.getStartBatteryPercent()) / 100.0 * booking.getBatteryCapacityKWh());
        double cost = unitsConsumed * station.getRatePerKWh();

        booking.setCost(cost);
        booking.markCompleted();
        station.freeSlot();

        String invoice = generateInvoice(booking, station, unitsConsumed);

        promoteNextInQueue(station);

        return invoice;
    }

    private void promoteNextInQueue(ChargingStation station) {
        Queue<Booking> queue = waitingQueues.get(station.getStationId());
        if (queue != null && !queue.isEmpty() && station.hasAvailableSlot()) {
            Booking next = queue.poll();
            next.markChargingStarted();
            station.occupySlot();
            System.out.println("[QUEUE] " + next.getBookingId() + " (" + next.getUserName()
                    + ") promoted from queue -> now charging at " + station.getStationId());
        }
    }

    private String generateInvoice(Booking booking, ChargingStation station, double unitsConsumed) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== INVOICE ====================\n");
        sb.append(String.format("Booking ID     : %s%n", booking.getBookingId()));
        sb.append(String.format("Customer       : %s (%s)%n", booking.getUserName(), booking.getVehicleNumber()));
        sb.append(String.format("Station        : %s - %s%n", station.getStationId(), station.getName()));
        sb.append(String.format("Battery        : %.0f%% -> %.0f%%%n", booking.getStartBatteryPercent(), booking.getCurrentBatteryPercent()));
        sb.append(String.format("Units consumed : %.2f kWh%n", unitsConsumed));
        sb.append(String.format("Rate           : Rs.%.2f / kWh%n", station.getRatePerKWh()));
        sb.append(String.format("Booked at      : %s%n", booking.getBookingTimeFormatted()));
        sb.append(String.format("Completed at   : %s%n", booking.getCompletionTimeFormatted()));
        sb.append("---------------------------------------------------\n");
        sb.append(String.format("TOTAL COST     : Rs.%.2f%n", booking.getCost()));
        sb.append("====================================================\n");
        return sb.toString();
    }

    public Booking findBooking(String bookingId) {
        return bookings.stream()
                .filter(b -> b.getBookingId().equalsIgnoreCase(bookingId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No such booking: " + bookingId));
    }

    public List<Booking> getChargingHistory(String vehicleNumber) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getVehicleNumber().equalsIgnoreCase(vehicleNumber) && b.getStatus() == BookingStatus.COMPLETED) {
                result.add(b);
            }
        }
        return result;
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Queue<Booking> getQueue(String stationId) {
        return waitingQueues.getOrDefault(stationId, new LinkedList<>());
    }

    public String adminDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ ADMIN DASHBOARD ================\n");
        int totalSlots = 0, occupied = 0;
        double totalRevenue = 0;
        for (ChargingStation s : stations) {
            totalSlots += s.getTotalSlots();
            occupied += (s.getTotalSlots() - s.getAvailableSlots());
        }
        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.COMPLETED) totalRevenue += b.getCost();
        }
        sb.append(String.format("Total stations   : %d%n", stations.size()));
        sb.append(String.format("Total slots      : %d (occupied: %d, free: %d)%n", totalSlots, occupied, totalSlots - occupied));
        sb.append(String.format("Total bookings   : %d%n", bookings.size()));
        long queued = bookings.stream().filter(b -> b.getStatus() == BookingStatus.QUEUED).count();
        long charging = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CHARGING).count();
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        sb.append(String.format("  Queued: %d | Charging: %d | Completed: %d%n", queued, charging, completed));
        sb.append(String.format("Total revenue    : Rs.%.2f%n", totalRevenue));
        sb.append("\nPer-station status:\n");
        for (ChargingStation s : stations) {
            int qsize = waitingQueues.getOrDefault(s.getStationId(), new LinkedList<>()).size();
            sb.append(String.format("  %s | waiting queue: %d%n", s, qsize));
        }
        sb.append("====================================================\n");
        return sb.toString();
    }

    /** Used by PersistenceManager after deserializing stations/bookings from disk. */
    public void loadData(List<ChargingStation> loadedStations, List<Booking> loadedBookings) {
        stations.clear();
        stations.addAll(loadedStations);
        bookings.clear();
        bookings.addAll(loadedBookings);
        waitingQueues.clear();
        for (ChargingStation s : stations) {
            waitingQueues.put(s.getStationId(), new LinkedList<>());
        }
        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.QUEUED) {
                waitingQueues.get(b.getStationId()).add(b);
            }
        }
        int maxStation = stations.stream().mapToInt(s -> parseNum(s.getStationId(), "ST")).max().orElse(0);
        int maxBooking = bookings.stream().mapToInt(b -> parseNum(b.getBookingId(), "BK")).max().orElse(1000);
        stationCounter.set(maxStation + 1);
        bookingCounter.set(Math.max(maxBooking + 1, 1001));
    }

    private int parseNum(String id, String prefix) {
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (Exception e) {
            return 0;
        }
    }
}
