import javax.swing.*;
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

    public ServerEventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, LinkedBlockingQueue<MyTextEvent> outgoingQueue, JTextArea serverTextArea, ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
        this.serverTextArea = serverTextArea;
        this.senderMap = senderMap;

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;
                    try {
                        serverTextArea.insert(tie.getText(), tie.getOffset());
                        outgoingQueue.put(tie);
                        syncSender(tie);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    try {
                        serverTextArea.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        outgoingQueue.put(tre);
                        syncSender(tre);
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
    private void syncSender(MyTextEvent event) throws InterruptedException{
        if (!compareHash(event)) {
            TextEventSender sender = senderMap.get(event);
            sender.put(new TextSyncEvent(serverTextArea.getText()));
        }
        senderMap.remove(event);
    }

    private boolean compareHash(MyTextEvent remoteEvent) {
        int localHash = serverTextArea.getText().hashCode();
        int remoteHash = remoteEvent.getTextHash();
        return localHash == remoteHash;


    }

}
