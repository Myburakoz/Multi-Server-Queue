import config.ServerConfig;
import config.SimConfig;

import java.util.Scanner;

import static util.TablePrinter.*;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    // ─── Clear Console ───
    public static void clearConsole() {
        try {
            if (System.getenv("TERM") != null) {
                // Real terminal: use system clear
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            } else {
                // IDE fallback: print blank lines
                for (int i = 0; i < 50; i++) System.out.println();
            }
        } catch (Exception e) {
            // Ultimate fallback
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    // ─── Styled prompt helpers ───
    private static void printBanner() {
        String bc = DIM + CYAN;
        int[] w = {52};
        System.out.println(topBorder(w, bc));
        printTitle("⚡  MULTI-SERVER QUEUE SIMULATION  ⚡", totalWidth(w), BOLD + BRIGHT_YELLOW);
        System.out.println(bottomBorder(w, bc));
        System.out.println();
    }

    private static String styledPrompt(String label) {
        return BRIGHT_CYAN + "  ▸ " + RESET + WHITE + label + RESET + BRIGHT_YELLOW + " ➜  " + RESET;
    }

    private static void sectionHeader(String title) {
        System.out.println();
        System.out.println(BOLD + BRIGHT_MAGENTA + "  ┌─ " + title + RESET);
        System.out.println(BOLD + BRIGHT_MAGENTA + "  │" + RESET);
    }

    private static void sectionFooter() {
        System.out.println(BOLD + BRIGHT_MAGENTA + "  │" + RESET);
        System.out.println(BOLD + BRIGHT_MAGENTA + "  └─ " + BRIGHT_GREEN + "✓ Done" + RESET);
    }

    private static int readInt(String label, int min, int max) {
        while (true) {
            try {
                System.out.print(styledPrompt(label + " (" + min + "-" + max + ")"));
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.println(BRIGHT_RED + "    ✗ Please enter a value between " + min + " and " + max + "." + RESET);
            } catch (NumberFormatException e) {
                System.out.println(BRIGHT_RED + "    ✗ Invalid input. Please enter a valid integer." + RESET);
            }
        }
    }

    private static int readIntMin(String label, int min) {
        while (true) {
            try {
                System.out.print(styledPrompt(label + " (≥" + min + ")"));
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min) return val;
                System.out.println(BRIGHT_RED + "    ✗ Value must be ≥ " + min + "." + RESET);
            } catch (NumberFormatException e) {
                System.out.println(BRIGHT_RED + "    ✗ Invalid input. Please enter a valid integer." + RESET);
            }
        }
    }

    // ─── Interactive Setup ───
    public static SimConfig interactiveSetup() {
        SimConfig cfg = new SimConfig();

        clearConsole();
        printBanner();

        // Number of customers
        sectionHeader("Customer Configuration");
        cfg.setNumCustomers(readInt("Number of customers", 1, 1000));
        sectionFooter();

        // Inter-arrival times
        sectionHeader("Inter-Arrival Time");
        while (true) {
            int minIA = readIntMin("Min inter-arrival", 1);
            int maxIA = readIntMin("Max inter-arrival", minIA);
            cfg.setMinInterarrivalTime(minIA);
            cfg.setMaxInterarrivalTime(maxIA);
            break;
        }
        sectionFooter();

        // Number of servers
        sectionHeader("Server Configuration");
        cfg.setNumServers(readInt("Number of servers", 1, 4));

        // Server service times
        ServerConfig[] sCfgs = new ServerConfig[cfg.getNumServers()];
        cfg.setServerCfg(sCfgs);
        for (int i = 0; i < cfg.getNumServers(); i++) {
            System.out.println(BOLD + BRIGHT_MAGENTA + "  │" + RESET);
            System.out.println(BOLD + BRIGHT_MAGENTA + "  │  " + BRIGHT_CYAN + "Server " + (i + 1) + RESET);
            sCfgs[i] = new ServerConfig();
            while (true) {
                int minS = readIntMin("  S" + (i + 1) + " Min service time", 1);
                int maxS = readIntMin("  S" + (i + 1) + " Max service time", minS);
                sCfgs[i].setMinServiceTime(minS);
                sCfgs[i].setMaxServiceTime(maxS);
                break;
            }
        }
        sectionFooter();

        System.out.println();
        System.out.println(BOLD + BRIGHT_GREEN + "  ✔ Setup complete! Running simulation..." + RESET);
        System.out.println();

        return cfg;
    }

    // ─── Interactive Menu ───
    public static void interactiveMenu(Simulation sim) {
        while (true) {
            String bc = DIM + CYAN;
            int[] w = {42};

            System.out.println(topBorder(w, bc));
            printTitle("📊  RESULTS MENU", totalWidth(w), BOLD + BRIGHT_YELLOW);
            System.out.println(midBorder(w, bc));

            String[][] items = {
                    {"1", "Inter-Arrival Time Distribution"},
                    {"2", "Service Time Distribution"},
                    {"3", "Customer Arrival Table"},
                    {"4", "Simulation Table"},
                    {"5", "Customer Table"},
                    {"6", "Customer Service Table (per server)"},
                    {"7", "Statistics"},
                    {"8", "Detailed Tick Log"},
                    {"9", "Print All Tables"},
                    {"0", "Exit"}
            };

            for (String[] item : items) {
                String numColor = item[0].equals("0") ? BRIGHT_RED : BRIGHT_GREEN;
                System.out.println(row(w,
                        new String[]{" [" + item[0] + "]  " + item[1]},
                        new String[]{numColor},
                        bc));
            }

            System.out.println(bottomBorder(w, bc));
            System.out.println();
            System.out.print(styledPrompt("Select an option"));

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                clearConsole();
                System.out.println(BOLD + BRIGHT_CYAN + "  Goodbye! 👋" + RESET);
                return;
            }

            clearConsole();

            boolean valid = true;
            switch (input) {
                case "1":
                    sim.printInterArrivalTable();
                    break;
                case "2":
                    sim.printServiceTable();
                    break;
                case "3":
                    sim.printArrivalTable();
                    break;
                case "4":
                    sim.printSimulationTable();
                    break;
                case "5":
                    sim.printCustomerTable();
                    break;
                case "6":
                    sim.printCustomerServiceTable();
                    break;
                case "7":
                    sim.printStatistics();
                    break;
                case "8":
                    sim.printDetailedLog();
                    break;
                case "9":
                    sim.printInterArrivalTable();
                    sim.printServiceTable();
                    sim.printArrivalTable();
                    sim.printSimulationTable();
                    sim.printCustomerTable();
                    sim.printCustomerServiceTable();
                    sim.printStatistics();
                    sim.printDetailedLog();
                    break;
                default:
                    System.out.println(BRIGHT_RED + "  ✗ Invalid option. Please try again." + RESET);
                    System.out.println();
                    valid = false;
                    break;
            }

            if (valid) {
                System.out.println();
                System.out.print(DIM + "  Press " + BRIGHT_CYAN + "Enter" + DIM + " to return to menu..." + RESET);
                scanner.nextLine();
                clearConsole();
            }
        }
    }

    // ─── Logging ───
    public static void saveResultsToLog(Simulation sim) {
        try {
            java.io.File logDir = new java.io.File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String filename = "logs/simulation_" + timestamp + ".log";

            java.io.PrintStream originalOut = System.out;

            java.io.PrintStream fileOut = new java.io.PrintStream(new java.io.FileOutputStream(filename)) {
                @Override
                public void print(String s) {
                    super.print(s == null ? null : s.replaceAll("\033\\[[0-9;]*m", ""));
                }
                @Override
                public void println(String s) {
                    super.println(s == null ? null : s.replaceAll("\033\\[[0-9;]*m", ""));
                }
                @Override
                public void print(Object obj) {
                    super.print(obj == null ? null : obj.toString().replaceAll("\033\\[[0-9;]*m", ""));
                }
                @Override
                public void println(Object obj) {
                    super.println(obj == null ? null : obj.toString().replaceAll("\033\\[[0-9;]*m", ""));
                }
            };

            System.setOut(fileOut);

            sim.printInterArrivalTable();
            sim.printServiceTable();
            sim.printArrivalTable();
            sim.printSimulationTable();
            sim.printCustomerTable();
            sim.printCustomerServiceTable();
            sim.printStatistics();
            sim.printDetailedLog();

            System.setOut(originalOut);
            fileOut.close();

            System.out.println(BRIGHT_GREEN + "  ✔ Results successfully saved to " + filename + RESET);
            System.out.println();
        } catch (Exception e) {
            System.out.println(BRIGHT_RED + "  ✗ Failed to save results to log file: " + e.getMessage() + RESET);
            System.out.println();
        }
    }

    // ─── Main ───
    public static void main(String[] args) {
        SimConfig cfg = interactiveSetup();

        Simulation sim = new Simulation(cfg);
        sim.run();

        saveResultsToLog(sim);
        
        System.out.print(DIM + "  Press " + BRIGHT_CYAN + "Enter" + DIM + " to open results menu..." + RESET);
        scanner.nextLine();

        clearConsole();
        interactiveMenu(sim);
    }
}
