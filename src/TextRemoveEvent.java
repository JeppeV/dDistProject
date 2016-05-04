public class TextRemoveEvent extends MyTextEvent {

    private int length;

    public TextRemoveEvent(String ipAddress, int timestamp, int offset, int length) {
        super(ipAddress, timestamp, offset);
        this.length = length;
    }

    public int getLength() {
        return length;
    }
}
