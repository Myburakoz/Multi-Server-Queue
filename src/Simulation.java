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

    // Output methods
    public void printInterArrivalTable() {
        System.out.println("-------------------------------------------------");
        System.out.println("          INTER-ARRIVAL TIME DISTRIBUTION        ");
        System.out.println("-------------------------------------------------");
        if (cfg == null) {
            System.out.println("Values are uniformly distributed between - and -.");
        } else {
            System.out.printf("Values are uniformly distributed between %d and %d.\n", cfg.getMinInterarrivalTime(), cfg.getMaxInterarrivalTime());
        }
        System.out.println("-------------------------------------------------\n");
    }

    public void printServiceTable() {
        System.out.println("-------------------------------------------------");
        System.out.println("       SERVICE TIME DISTRIBUTION (per server)    ");
        System.out.println("-------------------------------------------------");
        if (servers == null || servers.isEmpty()) {
            System.out.println("Server -: Uniformly distributed between - and -.");
        } else {
            for (Server s : servers) {
                System.out.printf("Server %d: Uniformly distributed between %d and %d.\n", s.getId(), s.getMinService(), s.getMaxService());
            }
        }
        System.out.println("-------------------------------------------------\n");
    }

    public void printArrivalTable() {
        System.out.println("-------------------------------------------------");
        System.out.println("             CUSTOMER ARRIVAL TABLE              ");
        System.out.println("-------------------------------------------------");
        System.out.printf("%-10s | %-15s | %-15s\n", "Customer", "Inter-Arrival", "Arrival Time");
        System.out.println("-------------------------------------------------");
        if (customers == null || customers.isEmpty()) {
            System.out.printf("%-10s | %-15s | %-15s\n", "-", "-", "-");
        } else {
            for (CustomerRecord c : customers) {
                System.out.printf("%-10d | %-15d | %-15d\n", c.getId(), c.getInterArrival(), c.getArrivalTime());
            }
        }
        System.out.println("-------------------------------------------------\n");
    }

    public void printSimulationTable() {
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.println("                                        SIMULATION TABLE                                                 ");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        int numServs = (cfg != null) ? cfg.getNumServers() : 3;
        StringBuilder sbHeaders = new StringBuilder();
        sbHeaders.append(String.format("%-6s | %-9s | ", "Clock", "Queue Len"));
        for (int i = 0; i < numServs; i++) {
            sbHeaders.append(String.format("S%-2d | ", i));
        }
        sbHeaders.append(String.format("%-30s | %-12s", "Future Event List", "Departures"));
        System.out.println(sbHeaders.toString());
        System.out.println("---------------------------------------------------------------------------------------------------------");

        if (tickStats == null || tickStats.isEmpty()) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-6s | %-9s | ", "-", "-"));
            for (int i = 0; i < numServs; i++) row.append(String.format("%-3s | ", "-"));
            row.append(String.format("%-30s | %-12s", "-", "-"));
            System.out.println(row.toString());
        } else {
            for (TickStat stat : tickStats) {
                StringBuilder row = new StringBuilder();
                row.append(String.format("%-6d | %-9d | ", stat.getClock(), stat.getQueueLen()));
                if (stat.getServerStatus() == null || stat.getServerStatus().isEmpty()) {
                    for (int i = 0; i < numServs; i++) row.append(String.format("%-3s | ", "-"));
                } else {
                    for (int sStatus : stat.getServerStatus()) {
                        row.append(String.format("%-3d | ", sStatus));
                    }
                }
                String fel = (stat.getFelStr() == null || stat.getFelStr().isEmpty()) ? "-" : stat.getFelStr();
                row.append(String.format("%-30s | %-12d", fel, stat.getNumDepartures()));
                System.out.println(row.toString());
            }
        }
        System.out.println("---------------------------------------------------------------------------------------------------------\n");
    }

    public void printStatistics() {
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.println("                                SIMULATION RESULTS / STATISTICS                                          ");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.printf("1. Average waiting time per customer            : %.4f minutes\n", avgWait);
        System.out.printf("2. Probability (wait)                           : %.4f\n", probWait);
        System.out.print ("3. Probability of idle server                   : ");
        if (probIdleServer != null) {
            for (int i = 0; i < probIdleServer.length; i++) {
                System.out.printf("[S%d: %.4f] ", i, probIdleServer[i]);
            }
            System.out.println();
        } else {
            System.out.println("[-]");
        }
        System.out.printf("4. Average service time                         : %.4f minutes\n", avgServiceActual);
        System.out.printf("5. Average time between arrivals                : %.4f minutes\n", avgInterArrActual);
        System.out.printf("6. Average waiting time of those who wait       : %.4f minutes\n", avgWaitThoseWhoWait);
        System.out.printf("7. Average time customer spends in the system   : %.4f minutes\n", avgSystemTime);
        System.out.println("---------------------------------------------------------------------------------------------------------\n");
    }

    public void printCustomerTable() {
        int numServs = (cfg != null) ? cfg.getNumServers() : 3;

        System.out.println("---------------------------------------------------------------------------------------------------------------------");
        System.out.println("                                           CUSTOMER TABLE                                                            ");
        System.out.println("---------------------------------------------------------------------------------------------------------------------");

        // Header row 1
        StringBuilder header1 = new StringBuilder();
        header1.append(String.format("%-10s | %-12s | ", "Customer", "Interarrival"));
        header1.append(String.format("%-12s | ", "Arrival"));
        for (int i = 0; i < numServs; i++) {
            header1.append(String.format("%-14s | ", "When S" + (i + 1)));
        }
        header1.append(String.format("%-13s | %-12s | %-12s | ", "Server", "Service", "Time"));
        for (int i = 0; i < numServs; i++) {
            header1.append(String.format("%-10s | ", "S" + (i + 1) + " Comp"));
        }
        header1.append(String.format("%-7s | %-11s", "Customer", "Time in"));
        System.out.println(header1.toString());

        // Header row 2
        StringBuilder header2 = new StringBuilder();
        header2.append(String.format("%-10s | %-12s | ", "Number", "Time (Min)"));
        header2.append(String.format("%-12s | ", "Time"));
        for (int i = 0; i < numServs; i++) {
            header2.append(String.format("%-14s | ", "Available"));
        }
        header2.append(String.format("%-13s | %-12s | %-12s | ", "Chosen", "Time (Min)", "Begins"));
        for (int i = 0; i < numServs; i++) {
            header2.append(String.format("%-10s | ", "Time"));
        }
        header2.append(String.format("%-7s | %-11s", "Delay", "System"));
        System.out.println(header2.toString());

        System.out.println("---------------------------------------------------------------------------------------------------------------------");

        if (customers == null || customers.isEmpty()) {
            System.out.println("-");
        } else {
            int[] serverAvail = new int[numServs];

            for (CustomerRecord c : customers) {
                StringBuilder row = new StringBuilder();
                row.append(String.format("%-10d | %-12s | ", c.getId(), String.valueOf(c.getInterArrival())));

                String arrival = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getArrivalTime());
                row.append(String.format("%-12s | ", arrival));

                for (int i = 0; i < numServs; i++) {
                    row.append(String.format("%-14s | ", String.valueOf(serverAvail[i])));
                }

                String server = c.getServerId() < 0 ? "-" : "S" + (c.getServerId() + 1);
                row.append(String.format("%-13s | ", server));

                String serviceTime = c.getServiceTime() <= 0 ? "-" : String.valueOf(c.getServiceTime());
                row.append(String.format("%-12s | ", serviceTime));

                String serviceStart = c.getServiceStartTime() < 0 ? "-" : String.valueOf(c.getServiceStartTime());

                row.append(String.format("%-12s | ", serviceStart));

                for (int i = 0; i < numServs; i++) {
                    if (c.getServerId() == i) {
                        row.append(String.format("%-10s | ", c.getServiceEndTime() < 0 ? "-" : String.valueOf(c.getServiceEndTime())));
                    } else {
                        row.append(String.format("%-10s | ", ""));
                    }
                }

                String wait = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getWaitTime());
                int sysTime = c.getWaitTime() + c.getServiceTime();
                String systemTime = c.getArrivalTime() < 0 ? "-" : String.valueOf(sysTime);

                row.append(String.format("%-7s | %-11s", wait, systemTime));

                System.out.println(row.toString());

                if (c.getServerId() >= 0 && c.getServerId() < numServs) {
                    serverAvail[c.getServerId()] = c.getServiceEndTime();
                }
            }
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------------\n");
    }

    // Core private methods
    private void insertEvent(Event ev) {
        this.FEL.add(ev);
    }

    private void generateArrivals() {
        int clock = 0;

        for(int i = 0; i < cfg.getNumCustomers(); i++){
            int interarrivalTime = ManualRandom.randRange(cfg.getMinInterarrivalTime(), cfg.getMaxInterarrivalTime());
            clock += interarrivalTime;

            CustomerRecord customer = customers.get(i);

            customer.setInterArrival(interarrivalTime);
            customer.setArrivalTime(clock);

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

        int serviceTime = server.nextServiceTime();

        if(currentClock > server.getLastFinish())
            server.increaseIdleTime(currentClock - server.getLastFinish());

        server.setBusy(true);
        server.setCurrentCustomer(custId);

        CustomerRecord customer = customers.get(custId);

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
                boolean eventOccurred = false;
    
                // A & B: GELİŞLER ve AYRILIŞLAR kontrolü
                while (!FEL.isEmpty() && FEL.peek().getTime() == clock) {
                    Event ev = FEL.poll();
                    if (ev.getEventType() == EventType.ARRIVAL) {
                        waitingList.offer(ev.getCustomerId());
                        eventOccurred = true;
                    } else if (ev.getEventType() == EventType.DEPARTURE) {
                        Server s = servers.get(ev.getServerId());
                        s.setBusy(false);
                        s.setLastFinish(clock);
                        s.setCompletedCount(s.getCompletedCount() + 1);
                        customersServed++;
                        eventOccurred = true;
                    }
                }
    
                // C. MASAYA (BOŞTA OLAN SUNUCUYA) ATAMA
                while (!waitingList.isEmpty()) {
                    int sid = findFreeServer();
                    if (sid < 0) break; // Boş sunucu kalmadı
    
                    int cid = waitingList.poll();
                    assignToServer(cid, sid, clock);
                    eventOccurred = true;
                }
    
                // D. LOG (TickStats) KAYDI
                if (eventOccurred) {
                    TickStat stat = new TickStat();
                    stat.setClock(clock);
                    stat.setQueueLen(waitingList.size());
                    
                    List<Integer> sStatus = new java.util.ArrayList<>();
                    for (Server s : servers) {
                        sStatus.add(s.isBusy() ? 1 : 0);
                    }
                    stat.setServerStatus(sStatus);
                    
                    PriorityQueue<Event> tempFEL = new PriorityQueue<>(FEL);
                    StringBuilder felBuilder = new StringBuilder();
                    int count = 0;
                    while (!tempFEL.isEmpty() && count < 4) {
                        Event fev = tempFEL.poll();
                        String t = fev.getEventType() == EventType.ARRIVAL ? "A" : "D";
                        felBuilder.append(String.format("(%s,%d,C%d) ", t, fev.getTime(), fev.getCustomerId()));
                        count++;
                    }
                    stat.setFelStr(felBuilder.toString().trim());
                    stat.setNumDepartures(customersServed);
                    
                    tickStats.add(stat);
                }
    
                // E. DOLULUK GÜNCELLEMESİ
                for (Server s : servers) {
                    if (s.isBusy()) {
                        s.setTotalBusyTime(s.getTotalBusyTime() + 1);
                    }
                }
    
                // F. BİTİŞ KONTROLÜ
                if (customersServed == cfg.getNumCustomers()) {
                    totalSimTime = clock;
                    break;
                }
            }
        }

    private void computeStatistics() {
        int numCustomers = cfg.getNumCustomers();

        if(numCustomers < 1) return;

        double totalWait = 0;
        double totalService = 0;
        double totalInterArrival = 0;
        double totalSystemTime = 0;

        int waitingCustomerCount  = 0;
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
