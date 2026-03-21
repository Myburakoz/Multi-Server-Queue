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
        /*
         * TODO: Simülasyon akışı:
         * 1. generateArrivals() metodunu çağırarak müşterilerin geliş zamanlarını oluşturun.
         * 2. simulate() metodunu çağırarak ana simülasyon döngüsünü çalıştırın.
         * 3. computeStatistics() metodunu çağırarak sonuç istatistiklerini hesaplayın.
         */

        generateArrivals();
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
        System.out.println("---------------------------------------------------------------------------------------------------------------------");
        System.out.println("                                           CUSTOMER TABLE                                                            ");
        System.out.println("---------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-12s | %-14s | %-13s | %-12s | %-11s | %-7s | %-11s\n",
                "Customer", "Arrival Time", "When Available", "Chosen Server", "Service Time", "Finish Time", "Delay", "System Time");
        System.out.println("---------------------------------------------------------------------------------------------------------------------");

        if (customers == null || customers.isEmpty()) {
            System.out.printf("%-10s | %-12s | %-14s | %-13s | %-12s | %-11s | %-7s | %-11s\n", "-", "-", "-", "-", "-", "-", "-", "-");
        } else {
            for (CustomerRecord c : customers) {
                String arrival = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getArrivalTime());
                String serviceStart = c.getServiceStartTime() < 0 ? "-" : String.valueOf(c.getServiceStartTime());
                String server = c.getServerId() < 0 ? "-" : String.valueOf(c.getServerId());
                String serviceTime = c.getServiceTime() <= 0 ? "-" : String.valueOf(c.getServiceTime());
                String serviceEnd = c.getServiceEndTime() < 0 ? "-" : String.valueOf(c.getServiceEndTime());
                String wait = c.getArrivalTime() < 0 ? "-" : String.valueOf(c.getWaitTime());
                int sysTime = c.getWaitTime() + c.getServiceTime();
                String systemTime = c.getArrivalTime() < 0 ? "-" : String.valueOf(sysTime);

                System.out.printf("%-10d | %-12s | %-14s | %-13s | %-12s | %-11s | %-7s | %-11s\n",
                        c.getId(), arrival, serviceStart, server, serviceTime, serviceEnd, wait, systemTime);
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
        /*
         * TODO: Ana simülasyon mantığı (Time-advance).
         * 1. Simülasyonun maksimum son saatini (endTime) bulun (customers içindeki en büyük arrivalTime'a + 200 ekleyebilirsiniz).
         * 2. customersServed (hizmet gören) = 0 gibi sayaçlar tutun.
         * 3. clock = 0'dan endTime'a kadar bir for döngüsü başlatın.
         * 4. Her bir 'clock' anı için:
         *    A. ARRIVALS (Gelişler) kontrolü:
         *       fel listesinin başındaki olayları kontrol edin. Eğer olay ARRIVAL ve zamanı clock'a eşit ise
         *       listeden çıkarın (Iterator kullanın) ve waitingList kuyruğuna (müşteri ID'sini) push edin.
         *    B. DEPARTURES (Ayrılışlar) kontrolü:
         *       Yine fel'de zamanı clock'a eşit olan DEPARTURE varsa listeden çıkartın.
         *       İlgili sunucunun busy flag'ini false yapın, lastFinish'i clock'a eşitleyin,
         *       completedCount'u 1 artırın. customersServed sayacını artırın.
         *    C. BOŞTA OLAN SUNUCUYA ATAMA:
         *       while (!waitingList.isEmpty()) {
         *           int sid = findFreeServer();
         *           if (sid < 0) break; // Boş sunucu kalmadı
         *           int cid = waitingList.poll();
         *           assignToServer(cid, sid, clock);
         *       }
         *    D. LOG (TickStats) KAYDI:
         *       Eğer o zaman diliminde herhangi bir olay(A, B veya C aşamasında) gerçekleştiyse,
         *       TickStat objesi oluşturup o anki saat, kuyruk uzunluğu, sunucu statüleri (busy durumu 0/1) ve
         *       kalan ilk 4 fel olayının özet stringini ekleyerek tickStats listesine kaydedin.
         *    E. Sunucuların "totalBusyTime" sayaçlarını her clock iterasyonunda, eğer busy=true ise artırın.
         *    F. Bitiş kontrolü: customersServed tüm müşterilere eşitse ve hem kuyruk hem de fel listesi
         *       sadece ayrılış içeriyor veya tamamen boşsa mevcut clock'u totalSimTime olarak kaydedip döngüyü (break) kırın.
         */
    }

    private void computeStatistics() {
        /*
         * TODO: Son istatistiklerin hesaplanması.
         * 1. customers listesini dolaşarak şu toplamları bulup ortalamalarını hesaplayın (Ortalama = Toplam / cfg.getNumCustomers()):
         *    - Toplam bekleme süresi -> avgWait (1)
         *    - Toplam servis süresi -> avgServiceActual (4)
         *    - Toplam inter-arrival süresi -> avgInterArrActual (5)
         *    - Toplam sistemde geçirdiği süre -> avgSystemTime (7)
         * 2. Bekleyen müşteri sayısını ve bekleyenlerin bekleme süresini ayrı bir sayaçta toplayıp:
         *    - probWait (Bekleme Olasılığı) = Bekleyen Sayısı / cfg.getNumCustomers() (2)
         *    - avgWaitThoseWhoWait = Bekleyenlerin Toplam Beklemesi / Bekleyen Sayısı (6)
         * 3. probIdleServer dizisini cfg.getNumServers() boyutunda yaratın. Her sunucu için "Boşta kalma yüzdesini" bulup diziye atayın (3):
         *    - Idle Olasılığı = (totalSimTime - server.getTotalBusyTime()) / (double) totalSimTime;
         */
    }
}
