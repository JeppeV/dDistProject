import java.io.Serializable;

/**
 * @author Jesper Buus Nielsen
 */
public class MyTextEvent implements Serializable {
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

    public void setTextHash(int hash){
        this.textHash = hash;
    }

    public int getTextHash(){
        return textHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MyTextEvent)) return false;

        MyTextEvent that = (MyTextEvent) o;

        if (offset != that.offset) return false;
        if (timestamp != that.timestamp) return false;
        return !(ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null);

    }

    @Override
    public int hashCode() {
        int result = offset;
        result = 31 * result + timestamp;
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        return result;
    }
}
