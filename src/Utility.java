import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Jeppe Vinberg on 23-05-2016.
 */
public abstract class Utility {

    public static  void startRunnable(Runnable runnable){
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static String getLocalHostAddress() {
        String address = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            address = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve Internet address of the local host");
            System.err.println(e);
            System.exit(-1);
        }
        return address;
    }

    public static Socket connectToServer(String serverAddress, int portNumber) {
        Socket socket = null;
        try {
            socket = new Socket(serverAddress, portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    public static ServerSocket registerOnPort(int portNumber) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.err.println("Cannot open server socket on port number" + portNumber);
            System.err.println(e);
            System.exit(-1);
        }
        return serverSocket;
    }

    public static Socket waitForConnectionFromClient(ServerSocket serverSocket) {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    public static void deregisterOnPort(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
