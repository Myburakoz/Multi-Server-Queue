package config;

public class SimConfig {
    private int numServers;
    private int numCustomers;
    private int minInterarrivalTime;
    private int maxInterarrivalTime;
    private ServerConfig[] serverCfg;

    public int getNumServers() {
        return numServers;
    }

    public void setNumServers(int numServers) {
        this.numServers = numServers;
    }

    public int getNumCustomers() {
        return numCustomers;
    }

    public void setNumCustomers(int numCustomers) {
        this.numCustomers = numCustomers;
    }

    public int getMinInterarrivalTime() {
        return minInterarrivalTime;
    }

    public void setMinInterarrivalTime(int minInterarrivalTime) {
        this.minInterarrivalTime = minInterarrivalTime;
    }

    public int getMaxInterarrivalTime() {
        return maxInterarrivalTime;
    }

    public void setMaxInterarrivalTime(int maxInterarrivalTime) {
        this.maxInterarrivalTime = maxInterarrivalTime;
    }

    public ServerConfig[] getServerCfg() {
        return serverCfg;
    }

    public void setServerCfg(ServerConfig[] serverCfg) {
        this.serverCfg = serverCfg;
    }
}
