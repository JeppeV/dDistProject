import java.io.Serializable;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class MyTextEvent implements Serializable {

    static final long serialVersionUID = 0L;
    private int offset;
    private int timestamp;

    protected MyTextEvent(int timestamp, int offset) {
        this.timestamp = timestamp;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public abstract int getLength();

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
