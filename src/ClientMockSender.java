import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 24/05/16.
 */
public class ClientMockSender implements EventSender {

    private LinkedBlockingQueue<MyTextEvent> events;
    private LamportClock lamportClock;

    public ClientMockSender(LinkedBlockingQueue<MyTextEvent> events, LamportClock lamportClock) {
        this.events = events;
        this.lamportClock = lamportClock;
    }

    @Override
    public void put(MyTextEvent event) throws InterruptedException {
        lamportClock.processTimestamp(event.getTimestamp());
        events.put(event);
    }
}
