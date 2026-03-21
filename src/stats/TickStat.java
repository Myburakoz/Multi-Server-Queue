package stats;

import java.util.List;

public class TickStat {
    private int clock;
    private int queueLen;
    private List<Integer> serverStatus;
    private String felStr;
    private int numDepartures;

    public int getClock() {
        return clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }

    public int getQueueLen() {
        return queueLen;
    }

    public void setQueueLen(int queueLen) {
        this.queueLen = queueLen;
    }

    public List<Integer> getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(List<Integer> serverStatus) {
        this.serverStatus = serverStatus;
    }

    public String getFelStr() {
        return felStr;
    }

    public void setFelStr(String felStr) {
        this.felStr = felStr;
    }

    public int getNumDepartures() {
        return numDepartures;
    }

    public void setNumDepartures(int numDepartures) {
        this.numDepartures = numDepartures;
    }
}
