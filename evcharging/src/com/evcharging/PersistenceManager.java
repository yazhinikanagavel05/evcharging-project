package com.evcharging;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PersistenceManager {
    private final String filePath;

    public PersistenceManager(String filePath) {
        this.filePath = filePath;
    }

    public void save(EVChargingSystem system) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(new ArrayList<>(system.getStations()));
            oos.writeObject(new ArrayList<>(system.getAllBookings()));
            System.out.println("[PERSIST] Data saved to " + filePath);
        } catch (IOException e) {
            System.out.println("[ERROR] Save failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void load(EVChargingSystem system) {
        File f = new File(filePath);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<ChargingStation> stations = (List<ChargingStation>) ois.readObject();
            List<Booking> bookings = (List<Booking>) ois.readObject();
            system.loadData(stations, bookings);
            System.out.println("[PERSIST] Loaded " + stations.size() + " station(s), " + bookings.size() + " booking(s) from " + filePath);
        } catch (Exception e) {
            System.out.println("[ERROR] Load failed: " + e.getMessage());
        }
    }
}
