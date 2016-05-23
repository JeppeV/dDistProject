import com.sun.corba.se.spi.activation.Server;

import javax.rmi.CORBA.Util;
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
        this.serverSocket = Utility.registerOnPort(localPort);
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = null;
        this.parent = null;
        this.senderManager = new SenderManager(incomingEvents, true);
        new Thread(senderManager).start();

    }

    public ConnectionManager(int localPort, JTextArea textArea, String IPAddress, int remotePort){
        this.serverSocket = Utility.registerOnPort(localPort);
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.parent = new Peer(IPAddress, remotePort);
        this.senderManager = new SenderManager(outgoingEvents, false);
        new Thread(senderManager).start();
        initParentConnection(IPAddress, remotePort);
    }

    @Override
    public void run() {
        Socket socket;
        while (true) {
            socket = Utility.waitForConnectionFromClient(serverSocket);
            if (socket != null) {
                System.out.println("New connection established to client " + socket);
                initClientThreads(socket);
            } else {
                System.out.println("ConnectionManager terminated");
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
        Socket rootSocket = Utility.connectToServer(IPAddress, portNumber);
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

    @Override
    public void disconnect() throws InterruptedException {
        if(parent != null){
            outgoingEvents.put(new RedirectEvent(parent));
            outgoingEvents.put(new ShutDownEvent(false));
        }
        incomingEvents.put(new ShutDownEvent(false));
        senderManager.clearSenders();
        Utility.deregisterOnPort(serverSocket);
        serverSocket = null;
        parent = null;
    }
}
