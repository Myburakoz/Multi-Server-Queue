package core;

public class Event implements Comparable<Event> {
    private EventType eventType;
    private int time; // Clock tick
    private int customerId;
    private int serverId;

    public Event(EventType ev, int time, int customerId, int serverId){
        this.eventType = ev;
        this.time = time;
        this.customerId = customerId;
        this.serverId = serverId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    @Override
    public int compareTo(Event obj){
        return Integer.compare(this.time, obj.time);
    }
}
