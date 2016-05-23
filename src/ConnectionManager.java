import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * For a peer acting as server, the resposibility of the ConnectionManager is to await
 * incoming connections from clients, and delegate a TextEventSender and TextEventCapturer to handle communication with
 * this socket. It is also responsible for initializing all server related processes and objects.
 */
public class ConnectionManager implements Runnable, DisconnectHandler {


    private ServerSocket serverSocket; // The ServerSocket related to this server
    private LinkedBlockingQueue<MyTextEvent> incomingEvents; // A shared queue for exchanging incoming events between threads
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private SenderManager senderManager; // A thread for managing the Sender threads of several clients
    private JTextArea textArea;
    private Peer parent;

    public ConnectionManager(int localPort, JTextArea textArea) {
        this.serverSocket = registerOnPort(localPort);
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = null;
        this.parent = null;
        this.senderManager = new SenderManager(incomingEvents, true);
        new Thread(senderManager).start();

    }

    public ConnectionManager(int localPort, JTextArea textArea, String IPAddress, int remotePort){
        this.serverSocket = registerOnPort(localPort);
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.parent = new Peer(IPAddress, remotePort);
        initParentConnection(IPAddress, remotePort);
        this.senderManager = new SenderManager(outgoingEvents, false);
        new Thread(senderManager).start();

    }

    @Override
    public void run() {
        Socket socket;
        while (true) {
            socket = waitForConnectionFromClient(serverSocket);
            if (socket != null) {
                System.out.println("New connection established to client " + socket);
                initClientThreads(socket);
            } else {
                System.out.println("Connection manager terminated");
                break;
            }
        }
    }

    private void initClientThreads(Socket socket) {
        TextEventSender sender = new TextEventSender(socket);
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        try {
            senderManager.addSender(sender, textArea);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initParentConnection(String IPAddress, int portNumber) {
        Socket rootSocket = connectToServer(IPAddress, portNumber);
        TextEventSender sender = new TextEventSender(rootSocket, incomingEvents);
        TextEventReceiver receiver = new TextEventReceiver(rootSocket, outgoingEvents, sender, this);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
    }

    public void redirectTo(Peer peer){
        initParentConnection(peer.getIPAddress(), peer.getPortNumber());
        parent = peer;
    }

    private Socket connectToServer(String serverAddress, int portNumber) {
        Socket socket = null;
        try {
            socket = new Socket(serverAddress, portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private ServerSocket registerOnPort(int portNumber) {
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

    private void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private Socket waitForConnectionFromClient(ServerSocket serverSocket) {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    @Override
    public void disconnect() throws InterruptedException {
        if(parent != null){
            outgoingEvents.put(new RedirectEvent(parent));
            outgoingEvents.put(new ShutDownEvent(false));
        }
        incomingEvents.put(new ShutDownEvent(false));
        senderManager.clearSenders();
        deregisterOnPort();
        parent = null;
    }
}
