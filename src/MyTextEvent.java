import java.io.Serializable;

/**
 * @author Jesper Buus Nielsen
 */
public abstract class MyTextEvent implements Serializable {

    static final long serialVersionUID = 0L;
    private int offset;
    private int timestamp;
    private String ipAddress;
    private int textHash;

    protected MyTextEvent(String ipAddress, int timestamp, int textHash, int offset) {
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.offset = offset;
        this.textHash = textHash;
    }

    public int getOffset() {
        return offset;
    }

    public int getTextHash() {
        return textHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyTextEvent event = (MyTextEvent) o;

        if (textHash != event.textHash) return false;
        return ipAddress != null ? ipAddress.equals(event.ipAddress) : event.ipAddress == null;

    }

    @Override
    public int hashCode() {
        int result = ipAddress != null ? ipAddress.hashCode() : 0;
        result = 31 * result + textHash;
        return result;
    }

    public int getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(int timestamp){
        this.timestamp = timestamp;
    }

    public abstract int getLength();

    public void setOffset(int offset){
        this.offset = offset;
    }
}
