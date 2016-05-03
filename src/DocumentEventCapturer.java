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
    private int lastInsertStringLength, lastRemoveStringLength;

    public DocumentEventCapturer() {
        this.enabled = true;
        this.eventHistory = new LinkedBlockingQueue<>();
        this.localOffset = -1;
        this.lastInsertStringLength = 0;
        this.lastRemoveStringLength = 0;
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
            TextInsertEvent event = new TextInsertEvent(localOffset, str);
            eventHistory.add(event);
        } else {
            super.insertString(fb, offset, str, a);
        }


    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        if (enabled) {
            if(offset != (localOffset + lastRemoveStringLength)){
                localOffset = offset;
            }
            TextRemoveEvent event = new TextRemoveEvent(localOffset, length);
            localOffset -= length;
            lastRemoveStringLength = length;
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
            if (length > 0) {
                localOffset = offset;
                event = new TextRemoveEvent(localOffset, length);
                eventHistory.add(event);
            }

            if(offset != (localOffset - lastInsertStringLength)){
                localOffset = offset;
            }

            event = new TextInsertEvent(localOffset, str);
            localOffset += str.length();
            lastInsertStringLength = str.length();
            eventHistory.add(event);

            System.out.println("Replace called locally");
            System.out.println("Local offset is: " + localOffset);

        } else {
            super.replace(fb, offset, length, str, a);
        }


    }

}
