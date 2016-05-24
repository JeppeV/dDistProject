import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 24/05/16.
 */
public class LocalClientHandler {

    private LamportClock lamportClock;
    private DocumentEventCapturer documentEventCapturer;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private EventReplayer eventReplayer;

    public LocalClientHandler(JTextArea textArea, LinkedBlockingQueue<MyTextEvent> outgoingEvents) {
        this.lamportClock = new LamportClock();
        this.documentEventCapturer = new DocumentEventCapturer(lamportClock, outgoingEvents);
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentEventCapturer);
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.eventReplayer = new EventReplayer(incomingEvents, textArea, documentEventCapturer);
    }

    public EventSender startLocalClient() {
        Utility.startRunnable(eventReplayer);
        return new ClientMockSender(incomingEvents, lamportClock);
    }
}
