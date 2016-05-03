import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DocumentFilter;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In out case we just use it to see all
 * the events and make a copy.
 *
 * @author Jesper Buus Nielsen
 */
public class DocumentEventCapturer extends DocumentFilter {

    private boolean enabled;
    private LinkedBlockingQueue<MyTextEvent> eventHistory;
    private int localOffset;
    private int lastOffset;

    public DocumentEventCapturer() {
        this.enabled = true;
        this.eventHistory = new LinkedBlockingQueue<>();
        this.localOffset = -1;
    }

    public LinkedBlockingQueue<MyTextEvent> getEventHistory() {
        return eventHistory;
    }

    public void clear() {
        eventHistory.clear();
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }


    public void insertString(FilterBypass fb, int offset,
            String str, AttributeSet a)
            throws BadLocationException {

        if (enabled) {
            if(localOffset < 0 || offset < localOffset) localOffset = offset;
            TextInsertEvent event = new TextInsertEvent(localOffset, str);
            localOffset += str.length();
            eventHistory.add(event);
        } else {
            super.insertString(fb, offset, str, a);
        }


    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        if (enabled) {
            localOffset = offset;
            TextRemoveEvent event = new TextRemoveEvent(localOffset, length);
            eventHistory.add(event);
            System.out.println("Removal called locally");
            System.out.println("Local offset is: " + localOffset);
        } else {
            super.remove(fb, offset, length);
        }

    }

    public void replace(FilterBypass fb, int offset,
            int length,
            String str, AttributeSet a)
            throws BadLocationException {
        MyTextEvent event;
        if (enabled) {
            if(localOffset != offset) localOffset = offset;
            if (length > 0) {
                event = new TextRemoveEvent(localOffset, length);
                eventHistory.add(event);
            }

            event = new TextInsertEvent(localOffset, str);
            eventHistory.add(event);
            localOffset += str.length();
            System.out.println("Replace called locally");
            System.out.println("Local offset is: " + localOffset);

        } else {
            super.replace(fb, offset, length, str, a);
        }


    }

}
