package core;

import util.ManualRandom;

public class Server {
    private int id;
    private boolean busy;
    private int currentCustomer;
    private int totalBusyTime;
    private int idleTime;
    private int lastFinish;
    private int completedCount;
    private int minService;
    private int maxService;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public int getCurrentCustomer() {
        return currentCustomer;
    }

    public void setCurrentCustomer(int currentCustomer) {
        this.currentCustomer = currentCustomer;
    }

    public int getTotalBusyTime() {
        return totalBusyTime;
    }

    public void setTotalBusyTime(int totalBusyTime) {
        this.totalBusyTime = totalBusyTime;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public int getLastFinish() {
        return lastFinish;
    }

    public void setLastFinish(int lastFinish) {
        this.lastFinish = lastFinish;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getMinService() {
        return minService;
    }

    public void setMinService(int minService) {
        this.minService = minService;
    }

    public int getMaxService() {
        return maxService;
    }

    public void setMaxService(int maxService) {
        this.maxService = maxService;
    }

    public int nextServiceTime() {
        int min = this.getMinService();
        int max = this.getMaxService();

        return ManualRandom.randRange(min, max);
    }
}
