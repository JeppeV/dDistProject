package main;

import events.MyTextEvent;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * This TextEvent is a special implementation of events.MyTextEvent that allows for a clean shutdown of communication between peers.
 * How its used:
 * When a peer wishes to disconnect, they put a ShutDownEvent onto the DocumentEventCapturers queue,
 * with a shutdown boolean value of false.
 * The result is that the TextEventSender sees this event and reads the boolean as false.
 * This is an indication to the Sender, that it should keep its socket open, yet terminate the run() method.
 * The TextEventReceiver of the other peer receives this value and reads the boolean value.
 * If it is false, it will keep its socket open, change the boolean value to true, and send the ShutDownEvent
 * directly to the other peer's TextEventSender.
 * The other peer's TextEventSender takes the element from its queue, sends it back to the first peer,
 * reads the boolean value as true and uses this as an indication that it should terminate and close the socket.
 * Lastly, the first peer's TextEventReceiver receives the ShutDownEvent, reads the boolean value as true
 * which prompts the run() method to close the socket and return.
 */
public class ShutDownEvent extends MyTextEvent {

    static final long serialVersionUID = 4L;
    private boolean shutdown;

    public ShutDownEvent(boolean shutdown) {
        super(-1, 0);
        this.shutdown = shutdown;
    }

    public void setShutdown(boolean s) {
        this.shutdown = s;
    }

    public boolean getShutdown() {
        return shutdown;
    }

    @Override
    public int getLength() {
        return 0;
    }
}
