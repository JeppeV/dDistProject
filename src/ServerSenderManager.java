import javax.swing.*;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 30-04-2016.
 * <p>
 * The resposibility of this class is to manage the sender theads for several clients.
 * If an event is put onto the incomingEvents queue, this runnable will distribute this event to each
 * of its associated TextEventSenders.
 * When a new TextEventSender is added, all of the servers text area is sent to the client as the first event.
 */
public class ServerSenderManager implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private LinkedBlockingQueue<TextEventSender> senders;
    private HashMap<Integer, MyTextEvent> eventLog;
    private int maxReceivedTimestamp;

    public ServerSenderManager(LinkedBlockingQueue<MyTextEvent> incomingEvents) {
        this.incomingEvents = incomingEvents;
        this.senders = new LinkedBlockingQueue<>();
        this.eventLog = new HashMap<>();
        this.maxReceivedTimestamp = -1;
    }

    @Override
    public void run() {
        MyTextEvent event;
        try {
            while (true) {
                event = incomingEvents.take();
                event = adjustOffset(event);
                for (TextEventSender sender : senders) {
                    sender.put(event);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addSender(TextEventSender sender, JTextArea area) throws InterruptedException {
        //send all of text area to new client
        sender.put(new TextInsertEvent("", -1, 0, 0, area.getText()));
        senders.put(sender);
    }

    private MyTextEvent adjustOffset(MyTextEvent event) {
        MyTextEvent e;
        System.out.println("______________________________________");
        System.out.println("Received event with timestamp: " + event.getTimestamp());
        while ((e = eventLog.get(event.getTimestamp())) != null || (event.getTimestamp() < maxReceivedTimestamp)) {

            if (e != null) {
                System.out.println("Event with timestamp: " + event.getTimestamp() + " exists in queue.");
                if (e.getOffset() <= event.getOffset()) {
                    if (e instanceof TextInsertEvent) {
                        event.setOffset(event.getOffset() + e.getLength());
                    } else if (e instanceof TextRemoveEvent) {
                        event.setOffset(event.getOffset() - e.getLength());
                    }
                }
            } else {
                System.out.println("Incrementing timestamp: " + event.getTimestamp() + ", to each max timestamp: " + maxReceivedTimestamp);
            }
            event.setTimestamp(event.getTimestamp() + 1);
        }
        eventLog.put(event.getTimestamp(), event);
        maxReceivedTimestamp = Math.max(event.getTimestamp(), maxReceivedTimestamp);
        System.out.println("Inserted event with timestamp: " + event.getTimestamp());
        System.out.println("______________________________________");
        return event;
    }


}
