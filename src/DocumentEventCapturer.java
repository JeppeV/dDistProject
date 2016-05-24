import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is used on the clients text area to be able to capture events and send them to the server.
 * This only happens, however, if the boolean value 'enabled' is true.
 * This flag is used to indicate whether the edits in the text area are to be sent to the server or not.
 */
public class DocumentEventCapturer extends DocumentFilter {

    private boolean enabled;
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private LamportClock lamportClock;

    public DocumentEventCapturer(LamportClock lamportClock, LinkedBlockingQueue<MyTextEvent> outgoingEvents) {
        this.enabled = true;
        this.outgoingEvents = outgoingEvents;
        this.lamportClock = lamportClock;
    }

    // TODO potential problem calling this method
    public void reset() {
        outgoingEvents.clear();
        lamportClock.reset();
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }


    public void insertString(FilterBypass fb, int offset, String str, AttributeSet a) throws BadLocationException {

        if (enabled) {
            TextInsertEvent event = new TextInsertEvent(lamportClock.getTime(), offset, str);
            outgoingEvents.add(event);
        } else {
            super.insertString(fb, offset, str, a);
        }

    }

    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        if (enabled) {
            TextRemoveEvent event = new TextRemoveEvent(lamportClock.getTime(), offset, length);
            outgoingEvents.add(event);
        } else {
            super.remove(fb, offset, length);
        }

    }

    public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet a) throws BadLocationException {
        MyTextEvent event;
        if (enabled) {
            if (length > 0) {
                event = new TextRemoveEvent(lamportClock.getTime(), offset, length);
                outgoingEvents.add(event);
            }
            event = new TextInsertEvent(lamportClock.getTime() + 1, offset, str);
            outgoingEvents.add(event);

        } else {
            super.remove(fb, offset, length);
            super.insertString(fb, offset, str, a);
        }

    }

}
