package main;

import events.*;

import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * The responsibility of the ConnectionManager class is to listen for incoming connections,
 * and setup threads for handling communication with these threads.
 */
public class ConnectionManager implements Runnable {

    private ServerSocket serverSocket; // The ServerSocket related to this server
    private LinkedBlockingQueue<MyTextEvent> incomingEvents; // A shared queue for exchanging incoming events between threads
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private SenderManager senderManager; // A thread for managing the Sender threads of several clients
    private Peer parent; // A Peer object representing the parent peer of this peer
    private Peer me; // A Peer object representing this peer
    private int localPort;
    private TextEventSender toParentSender; // The TextEventSender instance responsible for sending events to the parent
    private JTextArea textArea;
    private LocalClientHandler localClientHandler;

    private EventSender redirectSender;


    /**
     * This constructor is used for the root peer.
     * @param localPort the port on which this peer listens for connections.
     * @param textArea the text area which content should be sent to newly connected clients.
     */
    public ConnectionManager(int localPort, JTextArea textArea) {
        init(localPort, textArea);
        this.outgoingEvents = null;
        this.parent = null;
        this.senderManager = new SenderManager(textArea, incomingEvents, true);
        Utility.startRunnables(senderManager);
        senderManager.addSender(localClientHandler);
        System.out.println("I am Root");
    }

    /**
     * This constructor is used for leaf peers.
     * @param localPort the port on which this peer listen for connections.
     * @param textArea the text area which content should be sent to newly connected clients.
     * @param IPAddress the IPAddress of the parent of this peer.
     * @param remotePort the portNumber of the parent of this peer.
     */
    public ConnectionManager(int localPort, JTextArea textArea, String IPAddress, int remotePort){
        init(localPort, textArea);
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.parent = new Peer(IPAddress, remotePort);
        this.senderManager = new SenderManager(textArea, outgoingEvents, false);
        Utility.startRunnables(senderManager);
        senderManager.addSender(localClientHandler);
        initParentConnection(IPAddress, remotePort);
        System.out.println("I am a Peer");
    }

    private void init(int localPort, JTextArea textArea) {
        this.textArea = textArea;
        this.me = new Peer(Utility.getLocalHostAddress(), localPort);
        this.localPort = localPort;
        this.serverSocket = Utility.registerOnPort(localPort);
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

    /**
     * A method for setting up threads to handle communication with a newly connected client,
     * and adding them to the SenderManager instance.
     * @param socket the socket connetion to the newly connected client.
     */
    private void initClientThreads(Socket socket) {
        TextEventSender sender = new TextEventSender(socket);
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender, this);
        Utility.startRunnables(sender, receiver);
        senderManager.addSender(sender);
    }


    /**
     * A method for setting up threads to handle communication with the parent peer.
     * @param IPAddress the IPAddress of the parent peer
     * @param portNumber the portNumber of the parent peer
     */
    private void initParentConnection(String IPAddress, int portNumber) {
        Socket rootSocket = Utility.connectToServer(IPAddress, portNumber);
        toParentSender = new TextEventSender(rootSocket, incomingEvents);
        TextEventReceiver receiver = new TextEventReceiver(rootSocket, outgoingEvents, toParentSender, this);
        Utility.startRunnables(toParentSender, receiver);
    }

    /**
     * A method used for handling redirecting leaf peers, that is, change their parent connection
     * @param peer
     */
    public void redirectTo(Peer peer){
        initParentConnection(peer.getIPAddress(), peer.getPortNumber());
        parent = peer;
    }

    public Peer getThisPeer() {
        return me;
    }

    /**
     * This method is used to begin the disconnect operation.
     * It will operate differently depending on whether this peer is root or not.
     * @throws InterruptedException
     */
    public void disconnect() throws InterruptedException {
        if(parent != null){
            outgoingEvents.put(new RedirectEvent(parent));
            //outgoingEvents.put(new ShutDownEvent(false));
            incomingEvents.put(new ShutDownEvent(false));
            localClientHandler.terminate();
            Utility.deregisterOnPort(serverSocket);
            serverSocket = null;
            parent = null;
        } else {
            handleRootDisconnect();
        }


    }

    /**
     * This method is used when this peer is root and the disconnect menu item is clicked.
     * In that case, a random child is chosen to be assigned as the new root
     * by sending a RootAssignEvent.
     */
    private void handleRootDisconnect() {
        redirectSender = senderManager.getRedirectSender();
        try {
            localClientHandler.terminate();
            if(redirectSender == null) {
                incomingEvents.put(new ShutDownEvent(false));
                Utility.deregisterOnPort(serverSocket);
            } else {
                redirectSender.put(new RootAssignEvent(0, false));
            }

        } catch (InterruptedException e) {}


    }

    /** This method is called when the root receives a RootAssignAckEvent from peer
     * that is about to become the new root.
     * The method redirects all other child peers to the new root peer and sends a new
     * RootAssignEvent to the new root which contains all the information he needs
     * in order to function as a root.
     * @param peer the Peer object representing the peer that is about to become root.
     */
    public void setNewRoot(Peer peer) {
        try{
            incomingEvents.put(new RedirectEvent(peer));
            int maxReceivedTimestamp = senderManager.getMaxReceivedTimestamp();
            ConcurrentHashMap<Integer,MyTextEvent> eventLog = senderManager.getEventLog();
            RootAssignEvent rae = new RootAssignEvent(maxReceivedTimestamp, true);
            rae.setEventLog(eventLog);
            redirectSender.put(rae);
            //redirectSender.put(new ShutDownEvent(false));
        } catch (InterruptedException e) {}
        Utility.deregisterOnPort(serverSocket);
        serverSocket = null;

    }

    /**
     * This method is called when the first RootAssignEvent is received from the parent peer.
     * It simply sends a RootAssignAckEvent to the root to tell him where to redirect his children.
     */
    public void beginInitAsRoot() throws InterruptedException {
        toParentSender.put(new RootAssignAckEvent(new Peer(Utility.getLocalHostAddress(), localPort)));
    }

    /**
     * This method is called when the second RootAssignEvent is received from the parent peer.
     * The core responsibility is to create a new SenderManager that contains the
     * TextEventSenders from the old SenderManager and the EventLog from root SenderManager
     * @param rae the second RootAssignEvent sent from the server, containing all the information
     *            needed for this peer to behave as the root peer
     */
    public void finishInitAsRoot(RootAssignEvent rae) {
        parent = null;
        outgoingEvents = null;
        toParentSender = null;
        LinkedBlockingQueue<EventSender> senders = senderManager.getSenders();
        senderManager = new SenderManager(textArea, incomingEvents, senders, rae);
        Utility.startRunnables(senderManager);
        System.out.println("I have become root.");
    }


}
