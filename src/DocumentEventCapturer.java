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
    private JTextArea textArea;
    private LamportClock lamportClock;

    public DocumentEventCapturer(String IPAddress, JTextArea textArea, LamportClock lamportClock) {
        this.enabled = true;
        this.eventHistory = new LinkedBlockingQueue<>();
        this.IPAddress = IPAddress;
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
            TextInsertEvent event = new TextInsertEvent(IPAddress, lamportClock.getTime(), getTextAreaHash(), offset, str);
            eventHistory.add(event);

        } else {
            super.insertString(fb, offset, str, a);
        }

    }

    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        if (enabled) {
            System.out.println("RemoveEvent with offset: " + offset + " and length: " + length);
            TextRemoveEvent event = new TextRemoveEvent(IPAddress, lamportClock.getTime(), getTextAreaHash(), offset, length);
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

                event = new TextRemoveEvent(IPAddress, lamportClock.getTime(), getTextAreaHash(), offset, length);
                eventHistory.add(event);
            }
            event = new TextInsertEvent(IPAddress, lamportClock.getTime() + 1, getTextAreaHash(), offset, str);
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
