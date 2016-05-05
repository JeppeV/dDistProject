/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * This TextEvent is a special implementation of MyTextEvent that allows for a clean shutdown of communication between peers.
 * How its used:
 * When a peer wishes to disconnect, they put a ShutDownTextEvent onto the DocumentEventCapturers queue,
 * with a shutdown boolean value of false.
 * The result is that the TextEventSender sees this event and reads the boolean as false.
 * This is an indication to the Sender, that it should keep its socket open, yet terminate the run() method.
 * The TextEventReceiver of the other peer receives this value and reads the boolean value.
 * If it is false, it will keep its socket open, change the boolean value to true, and send the ShutDownTextEvent
 * directly to the other peer's TextEventReceiver, using a shared reference to the DocumentEventCapturer.
 * The other peer's TextEventSender takes the element from the queue, sends it back to the first peer,
 * reads the boolean value as true and uses this as an indication that it should terminate and close the socket.
 * Lastly, the first peer's TextEventReceiver receive the ShutDownTextEvent, reads the boolean value as true
 * which prompts the run() method to close the socket and return.
 */
public class ShutDownTextEvent extends MyTextEvent {

    static final long serialVersionUID = 3L;
    private boolean shutdown;

    public ShutDownTextEvent(boolean shutdown) {
        super("", -1, 0, 0);
        this.shutdown = shutdown;
    }

    public void setShutdown(boolean s) {
        this.shutdown = s;
    }

    public boolean getShutdown() {
        return shutdown;
    }
}
