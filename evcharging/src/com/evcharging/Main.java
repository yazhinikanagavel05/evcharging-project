package com.evcharging;

import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        EVChargingSystem system = new EVChargingSystem();
        PersistenceManager persistence = new PersistenceManager("evcharging.dat");
        persistence.load(system);

        Scanner sc = new Scanner(System.in);
        printBanner();

        while (true) {
            System.out.print("ev-charge> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            // Split respecting quoted strings, e.g. ADD_STATION "MG Road Station" "MG Road" 3 12.5
            String[] parts = splitArgs(line);
            String cmd = parts[0].toUpperCase();

            try {
                switch (cmd) {
                    case "ADD_STATION": {
                        if (parts.length < 5) {
                            System.out.println("Usage: ADD_STATION name location totalSlots ratePerKWh");
                            break;
                        }
                        ChargingStation s = system.registerStation(
                                parts[1], parts[2], Integer.parseInt(parts[3]), Double.parseDouble(parts[4]));
                        System.out.println("Registered: " + s);
                        break;
                    }
                    case "LIST_STATIONS": {
                        if (system.getStations().isEmpty()) {
                            System.out.println("No stations registered yet.");
                        }
                        for (ChargingStation s : system.getStations()) {
                            System.out.println(s);
                        }
                        break;
                    }
                    case "BOOK": {
                        if (parts.length < 7) {
                            System.out.println("Usage: BOOK userName vehicleNumber stationId batteryCapacityKWh startPercent targetPercent");
                            break;
                        }
                        Booking b = system.bookSlot(parts[1], parts[2], parts[3],
                                Double.parseDouble(parts[4]), Double.parseDouble(parts[5]), Double.parseDouble(parts[6]));
                        if (b.getStatus() == BookingStatus.CHARGING) {
                            System.out.println("Booking " + b.getBookingId() + " -> slot assigned, charging started.");
                        } else {
                            System.out.println("Booking " + b.getBookingId() + " -> all slots busy, added to queue.");
                        }
                        break;
                    }
                    case "UPDATE_BATTERY": {
                        if (parts.length < 3) {
                            System.out.println("Usage: UPDATE_BATTERY bookingId currentBatteryPercent");
                            break;
                        }
                        system.updateBatteryPercent(parts[1], Double.parseDouble(parts[2]));
                        System.out.println("OK");
                        break;
                    }
                    case "COMPLETE": {
                        if (parts.length < 2) {
                            System.out.println("Usage: COMPLETE bookingId");
                            break;
                        }
                        String invoice = system.completeCharging(parts[1]);
                        System.out.println(invoice);
                        break;
                    }
                    case "STATUS": {
                        if (parts.length < 2) {
                            System.out.println("Usage: STATUS stationId");
                            break;
                        }
                        ChargingStation s = system.findStation(parts[1])
                                .orElseThrow(() -> new java.util.NoSuchElementException("No such station: " + parts[1]));
                        System.out.println(s);
                        Queue<Booking> queue = system.getQueue(parts[1]);
                        System.out.println("Waiting queue (" + queue.size() + "):");
                        for (Booking b : queue) System.out.println("  " + b);
                        break;
                    }
                    case "HISTORY": {
                        if (parts.length < 2) {
                            System.out.println("Usage: HISTORY vehicleNumber");
                            break;
                        }
                        var history = system.getChargingHistory(parts[1]);
                        if (history.isEmpty()) {
                            System.out.println("No completed sessions for " + parts[1]);
                        }
                        for (Booking b : history) System.out.println(b);
                        break;
                    }
                    case "DASHBOARD": {
                        System.out.println(system.adminDashboard());
                        break;
                    }
                    case "SAVE": {
                        persistence.save(system);
                        break;
                    }
                    case "LOAD": {
                        persistence.load(system);
                        break;
                    }
                    case "HELP": {
                        printHelp();
                        break;
                    }
                    case "EXIT":
                    case "QUIT": {
                        persistence.save(system);
                        System.out.println("Bye!");
                        return;
                    }
                    default:
                        System.out.println("Unknown command. Type HELP for a list of commands.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    /** Splits on whitespace, but keeps "quoted phrases" as a single argument. */
    private static String[] splitArgs(String line) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(line);
        while (m.find()) {
            tokens.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return tokens.toArray(new String[0]);
    }

    private static void printBanner() {
        System.out.println("===================================================");
        System.out.println("   EV Charging Station Management System (CLI)   ");
        System.out.println("===================================================");
        printHelp();
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  ADD_STATION name location totalSlots ratePerKWh");
        System.out.println("      e.g. ADD_STATION \"MG Road Station\" \"MG Road\" 2 12.5");
        System.out.println("  LIST_STATIONS");
        System.out.println("  BOOK userName vehicleNumber stationId batteryCapacityKWh startPercent targetPercent");
        System.out.println("      e.g. BOOK Arjun TN01AB1234 ST1 40 20 80");
        System.out.println("  UPDATE_BATTERY bookingId currentBatteryPercent   - simulate charging progress");
        System.out.println("  COMPLETE bookingId                              - end session, print invoice, free slot");
        System.out.println("  STATUS stationId                                - slot occupancy + waiting queue");
        System.out.println("  HISTORY vehicleNumber                           - completed sessions for a vehicle");
        System.out.println("  DASHBOARD                                       - admin overview (revenue, occupancy, queues)");
        System.out.println("  SAVE / LOAD                                     - persist / restore from disk");
        System.out.println("  EXIT / QUIT                                     - save and exit");
    }
}
