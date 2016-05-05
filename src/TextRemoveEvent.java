public class TextRemoveEvent extends MyTextEvent {

    private int length;

    public TextRemoveEvent(String ipAddress, int timestamp, int textHash, int offset, int length) {
        super(ipAddress, timestamp, textHash, offset);
        this.length = length;
    }

    public int getLength() {
        return length;
    }
}
