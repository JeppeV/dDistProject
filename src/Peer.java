/**
 * Created by frederik290 on 23/05/16.
 */
public class Peer {
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
