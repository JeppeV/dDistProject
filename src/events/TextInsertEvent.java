package events;

/**
 * @author Jesper Buus Nielsen
 */
public class TextInsertEvent extends MyTextEvent {

    static final long serialVersionUID = 1L;
    private String text;

    public TextInsertEvent(int timestamp, int offset, String text) {
        super(timestamp, offset);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public int getLength() {
        return text.length();
    }
}

