import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
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

    public DocumentEventCapturer() {
        this.enabled = true;
        this.eventHistory = new LinkedBlockingQueue<>();
    }

    public LinkedBlockingQueue<MyTextEvent> getEventHistory() {
        return eventHistory;
    }

    public void clear() {
        eventHistory.clear();
    }

    public void put(MyTextEvent textEvent) throws InterruptedException {
        eventHistory.put(textEvent);
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

	/* Queue a copy of the event and then modify the textarea */
        if (enabled) {
            super.insertString(fb, offset, str, a);
            TextInsertEvent event = new TextInsertEvent(offset, str);
            eventHistory.add(event);

        } else {
            super.insertString(fb, offset, str, a);
        }

    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
    /* Queue a copy of the event and then modify the textarea */
        if (enabled) {
            super.remove(fb, offset, length);
            TextRemoveEvent event = new TextRemoveEvent(offset, length);
            eventHistory.add(event);

        } else {
            super.remove(fb, offset, length);
        }

    }

    public void replace(FilterBypass fb, int offset,
            int length,
            String str, AttributeSet a)
            throws BadLocationException {
	/* Queue a copy of the event and then modify the text */
        MyTextEvent event;
        if (enabled) {
            super.replace(fb, offset, length, str, a);
            if (length > 0) {
                event = new TextRemoveEvent(offset, length);
                eventHistory.add(event);
            }
            event = new TextInsertEvent(offset, str);
            eventHistory.add(event);

        } else {
            super.replace(fb, offset, length, str, a);
        }

    }

}
