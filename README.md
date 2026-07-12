# EV Charging Station Management System — Java Console App

A console-based management system for EV charging stations, covering
station registration, slot booking, FIFO queueing when stations are full,
battery-based cost calculation, invoicing, charging history, and an admin
dashboard.

## Features

- **Register charging stations** — name, location, number of slots, rate per kWh
- **Book charging slots** — instantly assigned if a slot is free
- **Queue management** — if a station is full, the booking joins a FIFO
  waiting queue and is auto-promoted to charging the moment a slot frees up
- **Battery percentage tracking** — track start %, live %, and target % per booking
- **Charging cost calculation** — cost = (batteryDelta% × batteryCapacityKWh) × ratePerKWh
- **Invoice generation** — printed automatically when a session completes
- **Charging history** — all completed sessions, filterable by vehicle number
- **Admin dashboard** — total stations/slots, occupancy, revenue, and
  per-station queue lengths
- **Persistence** — `SAVE`/`LOAD` to/from disk (auto-saves on exit)

## How to Build & Run

```bash
cd evcharging
javac -d bin src/com/evcharging/*.java
cd bin
java com.evcharging.Main
```

## Example Session

```
ADD_STATION "MG Road Station" "MG Road" 2 12.5
BOOK Arjun TN01AB1234 ST1 40 20 80
UPDATE_BATTERY BK1001 80
COMPLETE BK1001
DASHBOARD
```
(Use quotes for any name/location containing spaces.)

## Project Structure

```
src/com/evcharging/
  ChargingStation.java     # a station: slots, occupancy, rate
  Booking.java              # one charging session/booking
  BookingStatus.java        # QUEUED / CHARGING / COMPLETED
  EVChargingSystem.java     # core business logic: booking, queueing, billing
  PersistenceManager.java   # save/load via Java serialization
  Main.java                 # console REPL / command parser
```

## Design Notes (useful for interview discussion)

- **Queue management** uses Java's `Queue` interface (`LinkedList` as the
  implementation) — a genuine FIFO ADT, one per station. When a session
  completes, `promoteNextInQueue()` polls the queue and starts the next
  booking automatically — a clean example of event-driven state transition.
- **Cost calculation** converts battery *percentage* into actual **kWh
  units** using each vehicle's battery capacity, so two vehicles charging
  the same percentage but different battery sizes are billed correctly —
  worth mentioning as a deliberate real-world modeling choice.
- **Booking states** form a simple state machine: `QUEUED → CHARGING → COMPLETED`.
  Be ready to discuss how you'd add a `CANCELLED` state or handle a user
  who abandons a queued booking.
- **Persistence** serializes the full station/booking lists with Java's
  built-in serialization — simple but not human-readable; a good "next
  step" answer is switching to JSON or a database (this is a natural
  bridge to talk about DBMS, which Zoho interviews often probe).
- **Concurrency**: the current version is single-threaded/single-user by
  design (simple for a console demo). If asked "how would you make this
  multi-user," a good answer is: synchronize `EVChargingSystem`'s
  mutating methods, or move to a client-server model with a DB backing
  the state.

## Ideas to Extend Further

- Add `CANCEL bookingId` for queued bookings that never get to charge.
- Add estimated charging time using a station's power rating (kW) in
  addition to cost.
- Add multiple pricing tiers (peak/off-peak rates).
- Wrap in a TCP server so multiple "kiosks" can connect concurrently.
- Swap Java serialization for JSON or a lightweight embedded DB (e.g. SQLite via JDBC).
