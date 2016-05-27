package main;

import events.MyTextEvent;
import events.ShutDownEvent;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 24/05/16.
 *
 * This class is a way to handle the local client instance at every peer.
 * The local client enters text into the text area, which is timestamped with a
 * LamportClock. The entered textEvents are captured by the DocumentEventCapturer
 * which sends events to the parent of this peer.
 * As this object implements the EventSender interface, we are able to add it to
 * this peer's SenderManager instance, and as such, it will receive all events
 * received propagated by this peer.
 * When such events are received, the LamportClock instance processes the timestamp,
 * and the event is put onto the 'incomingEvents' queue.
 * An EventReplayer thread takes events off of this queue and replays them in the
 * associated TextArea.
 */
public class LocalClientHandler implements EventSender {

    private LamportClock lamportClock;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private boolean terminated = false;
    private JTextArea textArea;

    public LocalClientHandler(JTextArea textArea, LinkedBlockingQueue<MyTextEvent> outgoingEvents) {
        this.textArea = textArea;
        this.lamportClock = new LamportClock();
        DocumentEventCapturer documentEventCapturer = new DocumentEventCapturer(lamportClock, outgoingEvents);
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentEventCapturer);
        this.incomingEvents = new LinkedBlockingQueue<>();
        EventReplayer eventReplayer = new EventReplayer(incomingEvents, textArea, documentEventCapturer);
        Utility.startRunnables(eventReplayer);
    }

    @Override
    public void put(MyTextEvent event) throws InterruptedException  {
        lamportClock.processTimestamp(event.getTimestamp());
        incomingEvents.put(event);
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * This method is called when we want to terminate this local client.
     * That is, we terminate the EventReplayer thread by passing it a ShutDownEvent,
     * we remove the DocumentEventCapturer from the TextArea, allowing the client to type freely
     * and we set the terminated flag.
     * @throws InterruptedException
     */
    public void terminate() throws InterruptedException {
        incomingEvents.put(new ShutDownEvent(false));
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(null);
        terminated = true;

    }
}
