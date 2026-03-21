# ⚡ Multi-Server Queue Simulation

A discrete-event simulation of a **multi-server queueing system** (M/M/c model) implemented in Java. Simulates a call center–like environment where customers arrive, wait in a shared queue, and are served by one of multiple servers with configurable service time ranges.

---

## 📋 Features

- **1–4 configurable servers**, each with independent service time ranges
- **Uniform random distribution** for both inter-arrival and service times
- **Future Event List (FEL)** using Java's `PriorityQueue` for event ordering
- **Single shared FIFO queue** across all servers
- **Interactive CLI setup** with styled ANSI prompts
- **Post-simulation menu** to view individual result tables
- **Colored ASCII box-drawing tables** for all output (MySQL-style)

---

## 🏗️ Project Structure

```
src/
├── Main.java                    # Entry point, interactive setup & results menu
├── Simulation.java              # Core simulation engine & styled output
├── config/
│   ├── SimConfig.java           # Global simulation parameters
│   └── ServerConfig.java        # Per-server service time config
├── core/
│   ├── Event.java               # FEL event (Arrival/Departure)
│   ├── EventType.java           # Enum: ARRIVAL, DEPARTURE
│   ├── Server.java              # Server state & statistics
│   └── CustomerRecord.java      # Per-customer tracking data
├── stats/
│   └── TickStat.java            # Snapshot of system state per clock tick
└── util/
    ├── ManualRandom.java         # Uniform random number generator
    └── TablePrinter.java         # ASCII box-drawing table renderer with ANSI colors
```

---

## 🚀 How to Run

### Compile

```bash
cd src
javac -d ../out/production/MultiServerQueue \
  util/ManualRandom.java util/TablePrinter.java \
  config/ServerConfig.java config/SimConfig.java \
  core/Event.java core/EventType.java core/Server.java core/CustomerRecord.java \
  stats/TickStat.java Simulation.java Main.java
```

### Run

```bash
cd ../out/production/MultiServerQueue
java Main
```

### Or with IntelliJ IDEA

Open the project, mark `src/` as Sources Root, and run `Main.java`.

---

## 🎮 Usage

### 1. Interactive Setup

The program will prompt you for:

| Parameter             | Range     | Description                           |
|-----------------------|-----------|---------------------------------------|
| Number of customers   | 1–1000    | Total customers to simulate           |
| Min inter-arrival     | ≥ 1       | Minimum time between arrivals         |
| Max inter-arrival     | ≥ min     | Maximum time between arrivals         |
| Number of servers     | 1–4       | Number of parallel servers            |
| Per-server min/max    | ≥ 1       | Service time range for each server    |

### 2. Results Menu

After simulation completes, an interactive menu appears:

| Key | Table                              |
|-----|------------------------------------|
| `1` | Inter-Arrival Time Distribution    |
| `2` | Service Time Distribution          |
| `3` | Customer Arrival Table             |
| `4` | Simulation Table (FEL snapshots)   |
| `5` | Customer Table (detailed tracking) |
| `6` | Statistics Summary                 |
| `7` | Print All Tables                   |
| `0` | Exit                               |

Press **Enter** after viewing a table to return to the menu.

---

## 📊 Output Statistics

The simulation computes the following KPIs:

1. **Average waiting time** per customer
2. **Probability of waiting** (proportion of customers who waited)
3. **Probability of idle server** (per server)
4. **Average service time** (actual)
5. **Average inter-arrival time** (actual)
6. **Average waiting time of those who wait**
7. **Average time in system** (wait + service)

---

## ⚙️ Simulation Algorithm

The simulation uses a **time-advance mechanism** with clock ticking:

```
For each clock tick:
  A. Process ARRIVALS from FEL → add to waiting queue
  B. Process DEPARTURES from FEL → free up servers
  C. Assign waiting customers to free servers (FIFO)
  D. Log system state (TickStat) if any event occurred
  E. Update server busy-time counters
  F. Check termination (all customers served)
```

---

## 🛠️ Technologies

- **Language:** Java (JDK 8+)
- **Random Distribution:** Uniform (`Math.random()`)
- **Data Structures:** `PriorityQueue` (FEL), `LinkedList` (waiting queue)
- **Output:** ANSI escape codes + Unicode box-drawing characters
