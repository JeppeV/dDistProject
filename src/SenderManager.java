import com.sun.org.apache.xml.internal.security.Init;

import javax.swing.*;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 30-04-2016.
 * <p>
 * The resposibility of this class is to manage the sender theads for several clients.
 * If an event is put onto the outgoingEvents queue, this runnable will distribute this event to each
 * of its associated TextEventSenders.
 * When a new TextEventSender is added, all of the servers text area is sent to the client as the first event.
 */
public class SenderManager implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private LinkedBlockingQueue<EventSender> senders;
    private HashMap<Integer, MyTextEvent> eventLog;
    private int maxReceivedTimestamp;
    private boolean isRoot;

    public SenderManager(LinkedBlockingQueue<MyTextEvent> outgoingEvents, boolean isRoot) {
        this.outgoingEvents = outgoingEvents;
        this.senders = new LinkedBlockingQueue<>();
        this.eventLog = new HashMap<>();
        this.maxReceivedTimestamp = -1;
        this.isRoot = isRoot;
    }

    @Override
    public void run() {
        MyTextEvent event;
        try {
            while (true) {
                event = outgoingEvents.take();

                if(isRoot){
                    event = adjustOffset(event);
                }

                adjustMaxReceivedTimestamp(event);

                for (EventSender sender : senders) {
                    sender.put(event);
                }
                if(event instanceof ShutDownEvent) {
                    System.out.println("SenderManager process terminated");
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addSender(EventSender sender, JTextArea area) throws InterruptedException {
        //send all of text area to new client
        sender.put(new InitTextEvent(maxReceivedTimestamp, area.getText()));
        senders.put(sender);
    }

    public void clearSenders() {
        senders.clear();
    }

    private void adjustMaxReceivedTimestamp(MyTextEvent event){
        maxReceivedTimestamp = Math.max(event.getTimestamp(), maxReceivedTimestamp);
    }

    private MyTextEvent adjustOffset(MyTextEvent event) {
        MyTextEvent e;
        while ((e = eventLog.get(event.getTimestamp())) != null || (event.getTimestamp() < maxReceivedTimestamp)) {
            if (e != null) {
                if (e.getOffset() <= event.getOffset()) {
                    if (e instanceof TextInsertEvent) {
                        event.setOffset(event.getOffset() + e.getLength());
                    } else if (e instanceof TextRemoveEvent) {
                        event.setOffset(event.getOffset() - e.getLength());
                    }
                }
            }
            event.setTimestamp(event.getTimestamp() + 1);
        }
        eventLog.put(event.getTimestamp(), event);
        return event;
    }


}
