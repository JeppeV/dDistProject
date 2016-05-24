package main;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peer)) return false;

        Peer peer = (Peer) o;

        if (portNumber != peer.portNumber) return false;
        return !(IPAddress != null ? !IPAddress.equals(peer.IPAddress) : peer.IPAddress != null);

    }

    @Override
    public int hashCode() {
        int result = IPAddress != null ? IPAddress.hashCode() : 0;
        result = 31 * result + portNumber;
        return result;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }
}
