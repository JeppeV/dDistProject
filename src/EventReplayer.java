import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


public class EventReplayer implements Runnable {

    private int reset, normal;
    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private JTextArea area;
    private DocumentEventCapturer dec;
    private ConcurrentHashMap<MyTextEvent,MyTextEvent> localBuffer;

    public EventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, JTextArea area, DocumentEventCapturer dec, ConcurrentHashMap<MyTextEvent,MyTextEvent> localBuffer) {
        this.incomingQueue = incomingQueue;
        this.area = area;
        this.dec = dec;
        this.localBuffer = localBuffer;

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
                            MyTextEvent localEvent = localBuffer.get(tie);
                            if(localEvent == null){
                                area.insert(tie.getText(), tie.getOffset());
                            }
                            normal++;
                            localBuffer.remove(tie);
                            dec.enable();
                        } catch (Exception e) {
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
                            MyTextEvent localEvent = localBuffer.get(tre);
                            if(localEvent == null){
                                area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                            }
                            normal++;
                            localBuffer.remove(tre);
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                } else if(mte instanceof TextSyncEvent) {
                    final TextSyncEvent tse = (TextSyncEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            int caretPosition = area.getCaretPosition();
                            area.setText(tse.getAreaText());
                            area.getCaret().setDot(caretPosition);
                            System.out.println("Text reset");
                            reset++;
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                }
                System.out.println("______________________________");
                System.out.println("Normal inserts: " + normal);
                System.out.println("Resets: " + reset);

            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

}
