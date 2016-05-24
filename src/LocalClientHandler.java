import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 24/05/16.
 */
public class LocalClientHandler implements EventSender {

    private LamportClock lamportClock;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;

    public LocalClientHandler(JTextArea textArea, LinkedBlockingQueue<MyTextEvent> outgoingEvents) {
        this.lamportClock = new LamportClock();
        DocumentEventCapturer documentEventCapturer = new DocumentEventCapturer(lamportClock, outgoingEvents);
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentEventCapturer);
        this.incomingEvents = new LinkedBlockingQueue<>();
        EventReplayer eventReplayer = new EventReplayer(incomingEvents, textArea, documentEventCapturer);
        Utility.startRunnable(eventReplayer);
    }

    @Override
    public void put(MyTextEvent event) throws InterruptedException  {
        lamportClock.processTimestamp(event.getTimestamp());
        incomingEvents.put(event);
    }
}
