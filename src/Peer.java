import java.io.Serializable;

/**
 * Created by frederik290 on 23/05/16.
 */
public class Peer implements Serializable {
    static final long serialVersionUID = 6L;
    private String IPAddress;
    private int portNumber;

    public Peer(String IPAddress, int portNumber){
        this.IPAddress = IPAddress;
        this.portNumber = portNumber;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }
}
