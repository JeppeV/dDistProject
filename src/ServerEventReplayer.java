import javax.swing.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A server side version of the EventReplayer.
 * The responsibility of this class is to take new incoming events,
 * inserting them into the authorative serverTextArea and sending the events to the clients.
 * Due to client side prediction of events, this class also compares the hash of the text area of the
 * author of each event to the hash of the authorative serverTextArea. If these are not equal,
 * the prediction of the client is wrong, and we send a TextSyncEvent with the whole text
 * of the serverTextArea.
 */
public class ServerEventReplayer implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private LinkedBlockingQueue<MyTextEvent> outgoingQueue;
    private JTextArea serverTextArea;
    private ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap;
    private HashMap<Integer,MyTextEvent> eventLog;
    private int maxReceivedTimestamp;

    public ServerEventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, LinkedBlockingQueue<MyTextEvent> outgoingQueue, JTextArea serverTextArea, ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
        this.serverTextArea = serverTextArea;
        this.senderMap = senderMap;
        this.eventLog = new HashMap<>();
        this.maxReceivedTimestamp = -1;


    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                TextEventSender sender = senderMap.get(mte);
                senderMap.remove(mte);
                mte = adjustOffset(mte);
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;
                    try {
                        serverTextArea.insert(tie.getText(), tie.getOffset());
                        outgoingQueue.put(tie);
                        //syncSender(tie, sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    try {
                        serverTextArea.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        outgoingQueue.put(tre);
                       // syncSender(tre, sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

    /**
     * If the client's text area is not the same as that on the server, send a TextSyncEvent to the client.
     * @param event the event sent by a client
     */
    private void syncSender(MyTextEvent event, TextEventSender sender) throws InterruptedException{
        if (!compareHash(event)) {
            sender.put(new TextSyncEvent(serverTextArea.getText()));
        }
    }

    private boolean compareHash(MyTextEvent remoteEvent) {
        int localHash = serverTextArea.getText().hashCode();
        int remoteHash = remoteEvent.getTextHash();
        return localHash == remoteHash;
    }

    private MyTextEvent adjustOffset(MyTextEvent event){
        MyTextEvent e;
        System.out.println("______________________________________");
        System.out.println("Received event with timestamp: " + event.getTimestamp());
        while ((e = eventLog.get(event.getTimestamp())) != null || (event.getTimestamp() < maxReceivedTimestamp)){

            if(e != null) {
                System.out.println("Event with timestamp: " + event.getTimestamp() + " exists in queue.");
                if (e.getOffset() <= event.getOffset()) {
                    if (e instanceof TextInsertEvent) {
                        event.setOffset(event.getOffset() + e.getLength());
                    } else if (e instanceof TextRemoveEvent) {
                        event.setOffset(event.getOffset() - e.getLength());
                    }
                }
            }else{
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
