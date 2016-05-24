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
    private LocalClientHandler localClientHandler;

    public ConnectionManager(int localPort, JTextArea textArea) {
        init(localPort, textArea);
        this.outgoingEvents = null;
        this.parent = null;
        this.senderManager = new SenderManager(incomingEvents, true);
        new Thread(senderManager).start();
        senderManager.addSender(localClientHandler.startLocalClient(), textArea);
        System.out.println("I am Root");


    }

    public ConnectionManager(int localPort, JTextArea textArea, String IPAddress, int remotePort){
        init(localPort, textArea);
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.parent = new Peer(IPAddress, remotePort);
        this.senderManager = new SenderManager(outgoingEvents, false);
        new Thread(senderManager).start();
        senderManager.addSender(localClientHandler.startLocalClient(), textArea);
        initParentConnection(IPAddress, remotePort);
        System.out.println("I am a Peer");
    }

    private void init(int localPort, JTextArea textArea) {
        this.serverSocket = Utility.registerOnPort(localPort);
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.localClientHandler = new LocalClientHandler(textArea, incomingEvents);
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
        Utility.startRunnable(sender);
        Utility.startRunnable(receiver);
        senderManager.addSender(sender, textArea);
    }

    public void initParentConnection(String IPAddress, int portNumber) {
        Socket rootSocket = Utility.connectToServer(IPAddress, portNumber);
        TextEventSender sender = new TextEventSender(rootSocket, incomingEvents);
        TextEventReceiver receiver = new TextEventReceiver(rootSocket, outgoingEvents, sender, this);
        Utility.startRunnable(sender);
        Utility.startRunnable(receiver);
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
        Utility.deregisterOnPort(serverSocket);
        serverSocket = null;
        parent = null;
    }
}
