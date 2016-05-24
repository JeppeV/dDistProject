package events;

public class TextRemoveEvent extends MyTextEvent {

    static final long serialVersionUID = 2L;
    private int length;

    public TextRemoveEvent(int timestamp, int offset, int length) {
        super(timestamp, offset);
        this.length = length;
    }

    @Override
    public int getLength() {
        return length;
    }
}
