package core;

public class CustomerRecord {
    private int id;
    private int interArrival;
    private int arrivalTime;
    private int serviceStartTime;
    private int serviceEndTime;
    private int serverId;
    private int waitTime;
    private int serviceTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInterArrival() {
        return interArrival;
    }

    public void setInterArrival(int interArrival) {
        this.interArrival = interArrival;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(int serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public int getServiceEndTime() {
        return serviceEndTime;
    }

    public void setServiceEndTime(int serviceEndTime) {
        this.serviceEndTime = serviceEndTime;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(int serviceTime) {
        this.serviceTime = serviceTime;
    }
}
