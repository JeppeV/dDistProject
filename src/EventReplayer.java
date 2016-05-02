import javax.swing.*;
import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 */
public class EventReplayer implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private JTextArea area;
    private DocumentEventCapturer dec;

    public EventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, JTextArea area, DocumentEventCapturer dec) {
        this.incomingQueue = incomingQueue;
        this.area = area;
        this.dec = dec;

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            area.replaceRange(null, tie.getOffset(), tie.getOffset() + tie.getText().length()-1);
                            area.replaceRange(tie.getText(), tie.getOffset(), tie.getOffset() + tie.getText().length()-1);
                            //area.insert(tie.getText(), tie.getOffset());
                            dec.enable();
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                            dec.enable();
                        } catch (Exception e) {
                            System.err.println(e);
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                }
            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

}
