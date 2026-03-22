package stats;

import java.util.ArrayList;
import java.util.List;

public class TickStat {
    private int clock;
    private int queueLen;
    private List<Integer> serverStatus;
    private String felStr;
    private int numDepartures;
    private List<String> eventLog;

    public TickStat() {
        this.eventLog = new ArrayList<>();
    }

    public void addLog(String message) {
        eventLog.add(message);
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public void setEventLog(List<String> eventLog) {
        this.eventLog = eventLog;
    }

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
