import config.ServerConfig;
import config.SimConfig;

import java.util.Scanner;

public class Main {
    public static SimConfig interactiveSetup() {
        SimConfig cfg = new SimConfig();
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- MULTI-SERVER QUEUE SIMULATION SETUP ---");

        // Number of customers
        while (true) {
            try {
                System.out.print("Number of customers (1-1000): ");
                cfg.setNumCustomers(Integer.parseInt(scanner.nextLine()));
                if (cfg.getNumCustomers() >= 1 && cfg.getNumCustomers() <= 1000) break;
                System.out.println("Invalid input. Please enter a value between 1 and 1000.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }

        // Inter-arrival times
        while (true) {
            try {
                System.out.print("Min inter-arrival: ");
                cfg.setMinInterarrivalTime(Integer.parseInt(scanner.nextLine()));
                System.out.print("Max inter-arrival: ");
                cfg.setMaxInterarrivalTime(Integer.parseInt(scanner.nextLine()));

                if (cfg.getMinInterarrivalTime() > 0 && cfg.getMaxInterarrivalTime() >= cfg.getMinInterarrivalTime()) break;
                System.out.println("Invalid input. Min must be > 0 and Max >= Min.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter valid integers.");
            }
        }

        // Number of servers
        while (true) {
            try {
                System.out.print("Number of servers (1-4): ");
                cfg.setNumServers(Integer.parseInt(scanner.nextLine()));
                if (cfg.getNumServers() >= 1 && cfg.getNumServers() <= 4) break;
                System.out.println("Invalid input. Please enter a value between 1 and 4.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }

        // Server configurations
        ServerConfig[] sCfgs = new ServerConfig[cfg.getNumServers()];
        cfg.setServerCfg(sCfgs);
        for (int i = 0; i < cfg.getNumServers(); i++) {
            sCfgs[i] = new ServerConfig();
            while (true) {
                try {
                    System.out.printf("Server %d - Min service time: ", i);
                    sCfgs[i].setMinServiceTime(Integer.parseInt(scanner.nextLine()));
                    System.out.printf("Server %d - Max service time: ", i);
                    sCfgs[i].setMaxServiceTime(Integer.parseInt(scanner.nextLine()));

                    if (sCfgs[i].getMinServiceTime() > 0 && sCfgs[i].getMaxServiceTime() >= sCfgs[i].getMinServiceTime()) break;
                    System.out.println("Invalid input. Min > 0 and Max >= Min required.");
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter valid integers.");
                }
            }
        }

        System.out.println("Setup complete!\n");
        return cfg;
    }

    public static void main(String[] args) {
        SimConfig cfg = interactiveSetup();

        Simulation sim = new Simulation(cfg);
        sim.run();

        sim.printInterArrivalTable();
        sim.printServiceTable();
        sim.printArrivalTable();
        sim.printSimulationTable();
        sim.printCustomerTable();
        sim.printStatistics();
    }
}
