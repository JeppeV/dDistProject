import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is used on the clients text area to be able to capture events and send them to the server.
 * This only happens, however, if the boolean value 'enabled' is true.
 * This flag is used to indicate whether the edits in the text area are to be sent to the server or not.
 */
public class DocumentEventCapturer extends DocumentFilter {

    private boolean enabled;
    private LinkedBlockingQueue<MyTextEvent> eventHistory;
    private String IPAddress;
    private ConcurrentHashMap<MyTextEvent, MyTextEvent> localBuffer;
    private JTextArea textArea;
    private LamportClock lamportClock;

    public DocumentEventCapturer(String IPAddress, ConcurrentHashMap<MyTextEvent, MyTextEvent> localBuffer, JTextArea textArea, LamportClock lamportClock) {
        this.enabled = true;
        this.eventHistory = new LinkedBlockingQueue<>();
        this.IPAddress = IPAddress;
        this.localBuffer = localBuffer;
        this.textArea = textArea;
        this.lamportClock = lamportClock;

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
            super.insertString(fb, offset, str, a);
            TextInsertEvent event = new TextInsertEvent(IPAddress, lamportClock.generateTimestamp(), getTextAreaHash(), offset, str);
            localBuffer.put(event, event);
            eventHistory.add(event);

        } else {
            super.insertString(fb, offset, str, a);
        }

    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        if (enabled) {
            super.remove(fb, offset, length);
            TextRemoveEvent event = new TextRemoveEvent(IPAddress, lamportClock.generateTimestamp(), getTextAreaHash(), offset, length);
            localBuffer.put(event, event);
            eventHistory.add(event);

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
                super.remove(fb, offset, length);
                event = new TextRemoveEvent(IPAddress, lamportClock.generateTimestamp(), getTextAreaHash(), offset, length);
                localBuffer.put(event, event);
                eventHistory.add(event);
            }
            super.insertString(fb, offset, str, a);
            event = new TextInsertEvent(IPAddress, lamportClock.generateTimestamp(), getTextAreaHash(), offset, str);
            localBuffer.put(event, event);
            eventHistory.add(event);

        } else {
            super.remove(fb, offset, length);
            super.insertString(fb, offset, str, a);
        }

    }

    private int getTextAreaHash() {
        return textArea.getText().hashCode();
    }

}
