import javax.swing.*;
import java.util.LinkedList;
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
    private LinkedList<LinkedList<MyTextEvent>> log;
    private LamportClock serverClock;

    public ServerEventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, LinkedBlockingQueue<MyTextEvent> outgoingQueue, JTextArea serverTextArea, ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap, LamportClock serverClock) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
        this.serverTextArea = serverTextArea;
        this.senderMap = senderMap;
        this.serverClock = serverClock;
        this.log = new LinkedList<>();

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                serverClock.processTimestamp(mte.getTimestamp());
                TextEventSender sender = senderMap.get(mte);
                mte = adjustOffset(mte);
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;
                    try {
                        serverTextArea.insert(tie.getText(), tie.getOffset());
                        outgoingQueue.put(tie);
                        syncSender(tie.getOffset() + tie.getText().length(), tie, sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    try {
                        serverTextArea.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        outgoingQueue.put(tre);
                        syncSender(tre.getOffset(), tre, sender);
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
    private void syncSender(int offset, MyTextEvent event, TextEventSender sender) throws InterruptedException {
        if (!compareHash(event)) {
            sender.put(new TextSyncEvent(offset, serverTextArea.getText()));
        }
        senderMap.remove(event);
    }

    private boolean compareHash(MyTextEvent remoteEvent) {
        int localHash = serverTextArea.getText().hashCode();
        int remoteHash = remoteEvent.getTextHash();
        return localHash == remoteHash;


    }

    private MyTextEvent adjustOffset(MyTextEvent event) {
        LinkedList<MyTextEvent> recentEvents;
        int offsetAdjustment = 0;
        if(event.getTimestamp() < log.size()){
            recentEvents = log.get(event.getTimestamp());
            for(MyTextEvent e : recentEvents){
                if(e.getOffset() <= event.getOffset()){
                    if(e instanceof TextInsertEvent){
                        offsetAdjustment += e.getLength();
                    }else if(e instanceof  TextRemoveEvent){
                        offsetAdjustment -= e.getLength();

                    }
                }

            }
            System.out.println("Offset adjusted by: " + offsetAdjustment);
            event.setOffset(event.getOffset() + offsetAdjustment);
        }else{
            recentEvents = new LinkedList<>();
            log.add(event.getTimestamp(), recentEvents);
        }
        recentEvents.add(event);

        return event;
    }

}
