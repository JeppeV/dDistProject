package events;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jeppe Vinberg on 24-05-2016.
 */
public class RootAssignEvent extends MyTextEvent {

    static final long serialVersionUID = 8L;

    private ConcurrentHashMap<Integer,MyTextEvent> eventLog;
    private boolean assignFinished;

    public RootAssignEvent(int maxReceivedTimestamp, boolean assignFinished){
        super(maxReceivedTimestamp, 0);
        this.eventLog = null;
        this.assignFinished = assignFinished;
    }

    public boolean assignIsFinished() {
        return assignFinished;
    }

    public void setEventLog(ConcurrentHashMap<Integer,MyTextEvent> eventLog){
        this.eventLog = eventLog;
    }

    public ConcurrentHashMap<Integer,MyTextEvent> getEventLog() {
        return eventLog;
    }

    @Override
    public int getLength() {
        return 0;
    }
}
