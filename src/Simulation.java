import config.SimConfig;
import core.CustomerRecord;
import core.Event;
import core.EventType;
import core.Server;
import stats.TickStat;
import util.ManualRandom;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static util.TablePrinter.*;

public class Simulation {
    private SimConfig cfg;
    private List<Server> servers;
    private List<CustomerRecord> customers;

    private PriorityQueue<Event> FEL; // Priority Queue for sorted events
    private Queue<Integer> waitingList; // Queue for waiting customers

    List<TickStat> tickStats;

    // KPIs (Requested output stats + internal total time)
    private int totalSimTime;
    private double avgWait;
    private double probWait;
    private double[] probIdleServer;
    private double avgServiceActual;
    private double avgInterArrActual;
    private double avgWaitThoseWhoWait;
    private double avgSystemTime;

    public Simulation(SimConfig cfg) {
        this.cfg = cfg;

        this.servers = new java.util.ArrayList<>();
        this.customers = new java.util.ArrayList<>();
        this.waitingList = new java.util.LinkedList<>();
        this.tickStats = new java.util.ArrayList<>();
        this.FEL = new java.util.PriorityQueue<>();

        config.ServerConfig[] sCfgs = cfg.getServerCfg();
        for (int i = 0; i < cfg.getNumServers(); i++) {
            Server server = new Server();
            server.setId(i);

            server.setMinService(sCfgs[i].getMinServiceTime());
            server.setMaxService(sCfgs[i].getMaxServiceTime());

            this.servers.add(server);
        }

        for (int i = 1; i <= cfg.getNumCustomers(); i++) {
            CustomerRecord customer = new CustomerRecord();
            customer.setId(i);
            this.customers.add(customer);
        }
    }

    public void run() {
        generateArrivals();
        simulate();
        computeStatistics();
    }

    // ═══════════════════════════════════════════════════════════════
    // OUTPUT METHODS (Styled)
    // ═══════════════════════════════════════════════════════════════

    public void printInterArrivalTable() {
        int[] w = { 45 };
        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("INTER-ARRIVAL TIME DISTRIBUTION", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));

        String msg;
        if (cfg == null) {
            msg = "Values are uniformly distributed between - and -.";
        } else {
            msg = String.format("Values are uniformly distributed between %s%d%s and %s%d%s.",
                    BRIGHT_MAGENTA, cfg.getMinInterarrivalTime(), RESET,
                    BRIGHT_MAGENTA, cfg.getMaxInterarrivalTime(), RESET);
        }
        // Manual row for colored inline text
        System.out.println(bc + "│" + RESET + " " + msg
                + repeat(" ", 45 - stripAnsi(msg).length()) + " " + bc + "│" + RESET);
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printServiceTable() {
        int[] w = { 45 };
        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("SERVICE TIME DISTRIBUTION (per server)", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));

        if (servers == null || servers.isEmpty()) {
            System.out.println(row(w, new String[] { "Server -: Uniformly distributed between - and -." }, WHITE, bc));
        } else {
            for (Server s : servers) {
                String msg = String.format("Server %s%d%s: Uniform between %s%d%s and %s%d%s.",
                        BRIGHT_GREEN, s.getId() + 1, RESET,
                        BRIGHT_MAGENTA, s.getMinService(), RESET,
                        BRIGHT_MAGENTA, s.getMaxService(), RESET);
                System.out.println(bc + "│" + RESET + " " + msg
                        + repeat(" ", 45 - stripAnsi(msg).length()) + " " + bc + "│" + RESET);
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printArrivalTable() {
        int[] w = { 12, 18, 18 };
        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("CUSTOMER ARRIVAL TABLE", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));
        System.out.println(row(w, new String[] { "Customer", "Inter-Arrival", "Arrival Time" },
                BOLD + BRIGHT_CYAN, bc));
        System.out.println(midBorder(w, bc));

        if (customers == null || customers.isEmpty()) {
            System.out.println(row(w, new String[] { "-", "-", "-" }, DIM + WHITE, bc));
        } else {
            for (CustomerRecord c : customers) {
                System.out.println(row(w,
                        new String[] { String.valueOf(c.getId()), String.valueOf(c.getInterArrival()),
                                String.valueOf(c.getArrivalTime()) },
                        new String[] { BRIGHT_WHITE, BRIGHT_MAGENTA, BRIGHT_GREEN },
                        bc));
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printSimulationTable() {
        int numServs = (cfg != null) ? cfg.getNumServers() : 3;

        // Build dynamic widths: Clock(6), QueueLen(9), S0..Sn(3 each), FEL(32),
        // Departures(12)
        int cols = 2 + numServs + 2;
        int[] w = new int[cols];
        String[] headers = new String[cols];

        w[0] = 6;
        headers[0] = "Clock";
        w[1] = 9;
        headers[1] = "Queue Len";
        for (int i = 0; i < numServs; i++) {
            w[2 + i] = 5;
            headers[2 + i] = "S" + (i + 1);
        }
        w[2 + numServs] = 55;
        headers[2 + numServs] = "Future Event List";
        w[3 + numServs] = 12;
        headers[3 + numServs] = "Departures";

        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("SIMULATION TABLE", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));

        // Header colors
        String[] hColors = new String[cols];
        hColors[0] = BOLD + BRIGHT_CYAN;
        hColors[1] = BOLD + BRIGHT_CYAN;
        for (int i = 0; i < numServs; i++)
            hColors[2 + i] = BOLD + BRIGHT_GREEN;
        hColors[2 + numServs] = BOLD + BRIGHT_CYAN;
        hColors[3 + numServs] = BOLD + BRIGHT_CYAN;

        System.out.println(row(w, headers, hColors, bc));
        System.out.println(midBorder(w, bc));

        if (tickStats == null || tickStats.isEmpty()) {
            String[] empty = new String[cols];
            for (int i = 0; i < cols; i++)
                empty[i] = "-";
            System.out.println(row(w, empty, DIM + WHITE, bc));
        } else {
            for (TickStat stat : tickStats) {
                String[] vals = new String[cols];
                vals[0] = String.valueOf(stat.getClock());
                vals[1] = String.valueOf(stat.getQueueLen());
                if (stat.getServerStatus() == null || stat.getServerStatus().isEmpty()) {
                    for (int i = 0; i < numServs; i++)
                        vals[2 + i] = "-";
                } else {
                    int idx = 0;
                    for (int sStatus : stat.getServerStatus()) {
                        vals[2 + idx] = String.valueOf(sStatus);
                        idx++;
                    }
                }
                String fel = (stat.getFelStr() == null || stat.getFelStr().isEmpty()) ? "-" : stat.getFelStr();
                vals[2 + numServs] = fel;
                vals[3 + numServs] = String.valueOf(stat.getNumDepartures());

                // Color: clock=white, queue=yellow, servers=green/red, FEL=magenta, deps=white
                String[] rColors = new String[cols];
                rColors[0] = BRIGHT_WHITE;
                rColors[1] = BRIGHT_YELLOW;
                for (int i = 0; i < numServs; i++) {
                    rColors[2 + i] = vals[2 + i].equals("1") ? BRIGHT_RED : BRIGHT_GREEN;
                }
                rColors[2 + numServs] = BRIGHT_MAGENTA;
                rColors[3 + numServs] = BRIGHT_WHITE;

                System.out.println(row(w, vals, rColors, bc));
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printStatistics() {
        int[] w = { 50, 35 };
        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("SIMULATION RESULTS / STATISTICS", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));
        System.out.println(row(w, new String[] { "Metric", "Value" }, BOLD + BRIGHT_CYAN, bc));
        System.out.println(midBorder(w, bc));

        System.out.println(row(w,
                new String[] { "1. Average waiting time per customer", String.format("%.4f min", avgWait) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));
        System.out.println(row(w,
                new String[] { "2. Probability (wait)", String.format("%.4f", probWait) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));

        // Idle server probabilities
        String idleStr;
        if (probIdleServer != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < probIdleServer.length; i++) {
                if (i > 0)
                    sb.append("  ");
                sb.append(String.format("S%d: %.4f", i + 1, probIdleServer[i]));
            }
            idleStr = sb.toString();
        } else {
            idleStr = "-";
        }
        System.out.println(row(w,
                new String[] { "3. Probability of idle server", idleStr },
                new String[] { WHITE, BRIGHT_MAGENTA }, bc));
        System.out.println(row(w,
                new String[] { "4. Average service time", String.format("%.4f min", avgServiceActual) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));
        System.out.println(row(w,
                new String[] { "5. Average time between arrivals", String.format("%.4f min", avgInterArrActual) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));
        System.out.println(row(w,
                new String[] { "6. Avg waiting time of those who wait",
                        String.format("%.4f min", avgWaitThoseWhoWait) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));
        System.out.println(row(w,
                new String[] { "7. Avg time customer spends in system", String.format("%.4f min", avgSystemTime) },
                new String[] { WHITE, BRIGHT_GREEN }, bc));

        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printCustomerTable() {
        int numServs = (cfg != null) ? cfg.getNumServers() : 3;

        // Columns: Customer(8), Interarrival(12), Arrival(8), WhenS1..Sn(8 each),
        // Server(8), Service(8), Begins(8), S1Comp..SnComp(8 each), Delay(7), System(7)
        int cols = 6 + numServs * 2 + 2;
        int[] w = new int[cols];
        String[] h1 = new String[cols]; // header row 1
        String[] h2 = new String[cols]; // header row 2

        int ci = 0;
        w[ci] = 10;
        h1[ci] = "Customer";
        h2[ci] = "Number";
        ci++;
        w[ci] = 14;
        h1[ci] = "Interarrival";
        h2[ci] = "Time(Min)";
        ci++;
        w[ci] = 10;
        h1[ci] = "Arrival";
        h2[ci] = "Time";
        ci++;
        for (int i = 0; i < numServs; i++) {
            w[ci] = 12;
            h1[ci] = "When S" + (i + 1);
            h2[ci] = "Available";
            ci++;
        }
        w[ci] = 10;
        h1[ci] = "Server";
        h2[ci] = "Chosen";
        ci++;
        w[ci] = 10;
        h1[ci] = "Service";
        h2[ci] = "Time(Min)";
        ci++;
        w[ci] = 10;
        h1[ci] = "Time";
        h2[ci] = "Begins";
        ci++;
        for (int i = 0; i < numServs; i++) {
            w[ci] = 10;
            h1[ci] = "S" + (i + 1) + " Comp";
            h2[ci] = "Time";
            ci++;
        }
        w[ci] = 10;
        h1[ci] = "Customer";
        h2[ci] = "Delay";
        ci++;
        w[ci] = 10;
        h1[ci] = "Time in";
        h2[ci] = "System";
        ci++;

        String bc = DIM + CYAN;

        System.out.println(topBorder(w, bc));
        printTitle("CUSTOMER TABLE", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));
        System.out.println(row(w, h1, BOLD + BRIGHT_CYAN, bc));
        System.out.println(row(w, h2, BOLD + CYAN, bc));
        System.out.println(midBorder(w, bc));

        if (customers == null || customers.isEmpty()) {
            String[] empty = new String[cols];
            for (int i = 0; i < cols; i++)
                empty[i] = "-";
            System.out.println(row(w, empty, DIM + WHITE, bc));
        } else {
            int[] serverAvail = new int[numServs];

            for (CustomerRecord c : customers) {
                String[] vals = new String[cols];
                String[] colors = new String[cols];
                int vi = 0;

                vals[vi] = String.valueOf(c.getId());
                colors[vi] = BRIGHT_WHITE;
                vi++;
                vals[vi] = String.valueOf(c.getInterArrival());
                colors[vi] = BRIGHT_MAGENTA;
                vi++;
                vals[vi] = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getArrivalTime());
                colors[vi] = BRIGHT_GREEN;
                vi++;

                for (int i = 0; i < numServs; i++) {
                    vals[vi] = String.valueOf(serverAvail[i]);
                    colors[vi] = BRIGHT_YELLOW;
                    vi++;
                }

                String server = c.getServerId() < 0 ? "-" : "S" + (c.getServerId() + 1);
                vals[vi] = server;
                colors[vi] = BRIGHT_CYAN;
                vi++;

                vals[vi] = c.getServiceTime() <= 0 ? "-" : String.valueOf(c.getServiceTime());
                colors[vi] = BRIGHT_MAGENTA;
                vi++;

                vals[vi] = c.getServiceStartTime() < 0 ? "-" : String.valueOf(c.getServiceStartTime());
                colors[vi] = BRIGHT_GREEN;
                vi++;

                for (int i = 0; i < numServs; i++) {
                    if (c.getServerId() == i) {
                        vals[vi] = c.getServiceEndTime() < 0 ? "-" : String.valueOf(c.getServiceEndTime());
                    } else {
                        vals[vi] = "";
                    }
                    colors[vi] = BRIGHT_RED;
                    vi++;
                }

                String wait = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getWaitTime());
                int sysTime = c.getWaitTime() + c.getServiceTime();
                String systemTime = c.getArrivalTime() < 0 ? "-" : String.valueOf(sysTime);

                vals[vi] = wait;
                colors[vi] = BRIGHT_YELLOW;
                vi++;
                vals[vi] = systemTime;
                colors[vi] = BRIGHT_WHITE;
                vi++;

                System.out.println(row(w, vals, colors, bc));

                if (c.getServerId() >= 0 && c.getServerId() < numServs) {
                    serverAvail[c.getServerId()] = c.getServiceEndTime();
                }
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printCustomerServiceTable() {
        int numServs = (cfg != null) ? cfg.getNumServers() : 1;
        String bc = DIM + CYAN;

        // Columns: Customer + one "Service Time" per server
        int cols = 1 + numServs;
        int[] w = new int[cols];
        String[] headers = new String[cols];

        w[0] = 12;
        headers[0] = "Customer";
        for (int i = 0; i < numServs; i++) {
            w[1 + i] = 18;
            headers[1 + i] = "Service Time S" + (i + 1);
        }

        System.out.println(topBorder(w, bc));
        printTitle("CUSTOMER SERVICE TABLE", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));

        // Header colors
        String[] hColors = new String[cols];
        hColors[0] = BOLD + BRIGHT_CYAN;
        for (int i = 0; i < numServs; i++)
            hColors[1 + i] = BOLD + BRIGHT_GREEN;

        System.out.println(row(w, headers, hColors, bc));
        System.out.println(midBorder(w, bc));

        if (customers == null || customers.isEmpty()) {
            String[] empty = new String[cols];
            for (int i = 0; i < cols; i++)
                empty[i] = "-";
            System.out.println(row(w, empty, DIM + WHITE, bc));
        } else {
            for (CustomerRecord c : customers) {
                String[] vals = new String[cols];
                String[] colors = new String[cols];

                vals[0] = String.valueOf(c.getId());
                colors[0] = BRIGHT_WHITE;

                for (int i = 0; i < numServs; i++) {
                    int[] preGen = c.getPreGeneratedServiceTimes();
                    if (preGen != null && preGen.length > i) {
                        vals[1 + i] = String.valueOf(preGen[i]);
                        if (c.getServerId() == i) {
                            colors[1 + i] = BRIGHT_MAGENTA;
                        } else {
                            colors[1 + i] = DIM + MAGENTA;
                        }
                    } else {
                        vals[1 + i] = "-";
                        colors[1 + i] = DIM + WHITE;
                    }
                }

                System.out.println(row(w, vals, colors, bc));
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    public void printDetailedLog() {
        String bc = DIM + CYAN;
        int[] w = { 80 };

        System.out.println(topBorder(w, bc));
        printTitle("DETAILED TICK-BY-TICK LOG", totalWidth(w), BRIGHT_YELLOW);
        System.out.println(midBorder(w, bc));

        if (tickStats == null || tickStats.isEmpty()) {
            System.out.println(row(w, new String[] { "No tick data available." }, DIM + WHITE, bc));
        } else {
            for (int i = 0; i < tickStats.size(); i++) {
                TickStat stat = tickStats.get(i);

                // Clock header
                String clockHeader = String.format("Clock = %d", stat.getClock());
                System.out.println(row(w, new String[] { BOLD + BRIGHT_GREEN + "▶ " + clockHeader + RESET },
                        new String[] { BRIGHT_GREEN }, bc));

                // Event log lines
                for (String logLine : stat.getEventLog()) {
                    System.out.println(row(w, new String[] { "    → " + logLine },
                            new String[] { WHITE }, bc));
                }

                // FEL line
                String felDisplay = "  FEL: " + (stat.getFelStr() == null ? "(empty)" : stat.getFelStr());
                System.out.println(row(w, new String[] { felDisplay },
                        new String[] { BRIGHT_MAGENTA }, bc));

                // Separator between ticks
                if (i < tickStats.size() - 1) {
                    System.out.println(row(w, new String[] { repeat("─", 80) },
                            new String[] { DIM + CYAN }, bc));
                }
            }
        }
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static String repeat(String s, int n) {
        if (n <= 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++)
            sb.append(s);
        return sb.toString();
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\033\\[[0-9;]*m", "");
    }

    // Core private methods
    private void insertEvent(Event ev) {
        this.FEL.add(ev);
    }

    private void generateArrivals() {
        int clock = 0;

        for (int i = 0; i < cfg.getNumCustomers(); i++) {
            int interarrivalTime = ManualRandom.randRange(cfg.getMinInterarrivalTime(), cfg.getMaxInterarrivalTime());
            clock += interarrivalTime;

            CustomerRecord customer = customers.get(i);

            customer.setInterArrival(interarrivalTime);
            customer.setArrivalTime(clock);

            // Pre-generate service times for all servers for this customer
            int[] preGenTimes = new int[servers.size()];
            for (int s = 0; s < servers.size(); s++) {
                preGenTimes[s] = servers.get(s).nextServiceTime();
            }
            customer.setPreGeneratedServiceTimes(preGenTimes);

            insertEvent(new Event(EventType.ARRIVAL, clock, i, -1));
        }
    }

    private int findFreeServer() {
        for (Server server : servers) {
            if (!server.isBusy()) {
                return server.getId();
            }
        }
        return -1;
    }

    private void assignToServer(int custId, int servId, int currentClock) {
        Server server = servers.get(servId);
        CustomerRecord customer = customers.get(custId);

        // Fetch the pre-generated service time for this specific server
        int serviceTime = 0;
        if (customer.getPreGeneratedServiceTimes() != null && servId < customer.getPreGeneratedServiceTimes().length) {
            serviceTime = customer.getPreGeneratedServiceTimes()[servId];
        } else {
            serviceTime = server.nextServiceTime(); // Fallback
        }

        if (currentClock > server.getLastFinish())
            server.increaseIdleTime(currentClock - server.getLastFinish());

        server.setBusy(true);
        server.setCurrentCustomer(custId);

        customer.setServiceStartTime(currentClock);
        customer.setServiceTime(serviceTime);
        customer.setServiceEndTime(currentClock + serviceTime);
        customer.setServerId(servId);
        customer.setWaitTime(currentClock - customer.getArrivalTime());

        insertEvent(new Event(EventType.DEPARTURE, currentClock + serviceTime, custId, servId));
    }

    private void simulate() {
        int maxArrivalTime = 0;
        for (CustomerRecord c : customers) {
            if (c.getArrivalTime() > maxArrivalTime) {
                maxArrivalTime = c.getArrivalTime();
            }
        }
        int endTime = maxArrivalTime + 200;

        int customersServed = 0;

        for (int clock = 0; clock <= endTime; clock++) {
            TickStat stat = new TickStat();
            stat.setClock(clock);
            boolean eventOccurred = false;

            // A & B: ARRIVALS and DEPARTURES
            while (!FEL.isEmpty() && FEL.peek().getTime() == clock) {
                Event ev = FEL.poll();
                if (ev.getEventType() == EventType.ARRIVAL) {
                    waitingList.offer(ev.getCustomerId());
                    stat.addLog("Customer " + (ev.getCustomerId() + 1) + " ARRIVED (added to waiting list)");
                    eventOccurred = true;
                } else if (ev.getEventType() == EventType.DEPARTURE) {
                    Server s = servers.get(ev.getServerId());
                    s.setBusy(false);
                    s.setLastFinish(clock);
                    s.setCompletedCount(s.getCompletedCount() + 1);
                    customersServed++;
                    stat.addLog("Customer " + (ev.getCustomerId() + 1) + " DEPARTED from Server S"
                            + (ev.getServerId() + 1));
                    eventOccurred = true;
                }
            }

            // C. ASSIGN TO FREE SERVER
            while (!waitingList.isEmpty()) {
                int sid = findFreeServer();
                if (sid < 0)
                    break;

                int cid = waitingList.poll();
                assignToServer(cid, sid, clock);
                CustomerRecord cr = customers.get(cid);
                stat.addLog("Customer " + (cid + 1) + " assigned to Server S" + (sid + 1)
                        + " (service=" + cr.getServiceTime() + ", ends at t=" + cr.getServiceEndTime() + ")");
                eventOccurred = true;
            }

            // D. STATE SNAPSHOT — server statuses
            List<Integer> sStatus = new java.util.ArrayList<>();
            StringBuilder serverSummary = new StringBuilder();
            for (int i = 0; i < servers.size(); i++) {
                Server s = servers.get(i);
                sStatus.add(s.isBusy() ? 1 : 0);
                if (i > 0)
                    serverSummary.append(" | ");
                if (s.isBusy()) {
                    serverSummary.append("S").append(i + 1).append("=BUSY(C").append(s.getCurrentCustomer() + 1)
                            .append(")");
                } else {
                    serverSummary.append("S").append(i + 1).append("=IDLE");
                }
            }
            stat.setServerStatus(sStatus);
            stat.setQueueLen(waitingList.size());

            // Queue contents
            if (!waitingList.isEmpty()) {
                StringBuilder qb = new StringBuilder();
                for (int wid : waitingList) {
                    if (qb.length() > 0)
                        qb.append(", ");
                    qb.append("C").append(wid + 1);
                }
                stat.addLog("Queue(" + waitingList.size() + "): [" + qb + "]  |  " + serverSummary);
            } else {
                stat.addLog("Queue(0): empty  |  " + serverSummary);
            }

            // FEL snapshot
            PriorityQueue<Event> tempFEL = new PriorityQueue<>(FEL);
            StringBuilder felBuilder = new StringBuilder();
            while (!tempFEL.isEmpty()) {
                Event fev = tempFEL.poll();
                String t = fev.getEventType() == EventType.ARRIVAL ? "A" : "D";
                felBuilder.append(String.format("(%s,t=%d,C%d) ", t, fev.getTime(), fev.getCustomerId() + 1));
            }
            String felStr = felBuilder.toString().trim();
            stat.setFelStr(felStr.isEmpty() ? "(empty)" : felStr);

            stat.setNumDepartures(customersServed);

            // Only record ticks where something happened (to keep log readable)
            if (eventOccurred) {
                tickStats.add(stat);
            }

            // E. BUSY TIME UPDATE
            for (Server s : servers) {
                if (s.isBusy()) {
                    s.setTotalBusyTime(s.getTotalBusyTime() + 1);
                }
            }

            // F. END CHECK
            if (customersServed == cfg.getNumCustomers()) {
                totalSimTime = clock;
                break;
            }
        }
    }

    private void computeStatistics() {
        int numCustomers = cfg.getNumCustomers();

        if (numCustomers < 1)
            return;

        double totalWait = 0;
        double totalService = 0;
        double totalInterArrival = 0;
        double totalSystemTime = 0;

        int waitingCustomerCount = 0;
        double totalWaitForThoseWhoWait = 0;

        for (CustomerRecord c : customers) {
            totalWait += c.getWaitTime();
            totalService += c.getServiceTime();
            totalInterArrival += c.getInterArrival();
            totalSystemTime += c.getWaitTime() + c.getServiceTime();

            if (c.getWaitTime() > 0) {
                waitingCustomerCount++;
                totalWaitForThoseWhoWait += c.getWaitTime();
            }
        }

        avgWait = totalWait / numCustomers;
        probWait = (double) waitingCustomerCount / numCustomers;
        avgServiceActual = totalService / numCustomers;
        avgInterArrActual = totalInterArrival / numCustomers;

        if (waitingCustomerCount > 0) {
            avgWaitThoseWhoWait = totalWaitForThoseWhoWait / waitingCustomerCount;
        } else {
            avgWaitThoseWhoWait = 0.0;
        }
        avgSystemTime = totalSystemTime / numCustomers;

        int numServers = cfg.getNumServers();
        probIdleServer = new double[numServers];

        if (totalSimTime > 0) {
            for (int i = 0; i < numServers; i++) {
                Server s = servers.get(i);
                probIdleServer[i] = (totalSimTime - s.getTotalBusyTime()) / (double) totalSimTime;
            }
        }
    }
}
