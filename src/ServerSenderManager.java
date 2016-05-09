import javax.swing.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 30-04-2016.
 *
 * The resposibility of this class is to manage the sender theads for several clients.
 * If an event is put onto the events queue, this runnable will distribute this event to each
 * of its associated TextEventSenders.
 * When a new TextEventSender is added, all of the servers text area is sent to the client as the first event.
 */
public class ServerSenderManager implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> events;
    private LamportClock serverClock;
    private LinkedBlockingQueue<TextEventSender> senders;

    public ServerSenderManager(LinkedBlockingQueue<MyTextEvent> events, LamportClock serverClock) {
        this.events = events;
        this.serverClock = serverClock;
        this.senders = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        MyTextEvent event;
        try {
            while (true) {
                event = events.take();
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
        sender.put(new TextInsertEvent("", serverClock.getTime(), 0, 0, area.getText()));
        senders.put(sender);
    }


}
