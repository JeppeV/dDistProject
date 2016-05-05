/**
 * @author Jesper Buus Nielsen
 */
public class TextInsertEvent extends MyTextEvent {

    private String text;

    public TextInsertEvent(String ipAddress, int timestamp, int textHash, int offset, String text) {
        super(ipAddress, timestamp, textHash, offset);
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

