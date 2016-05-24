package main;

import events.MyTextEvent;
import events.RedirectEvent;
import events.RootAssignAckEvent;
import events.RootAssignEvent;

import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
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
    private Peer parent, me;
    private int localPort;
    private TextEventSender toRootSender;
    private JTextArea textArea;

    private EventSender redirectSender;
    private LocalClientHandler localClientHandler;

    public ConnectionManager(int localPort, JTextArea textArea) {
        init(localPort, textArea);
        this.outgoingEvents = null;
        this.parent = null;
        this.senderManager = new SenderManager(textArea, incomingEvents, true);
        new Thread(senderManager).start();
        senderManager.addSender(localClientHandler);
        System.out.println("I am Root");
    }

    public ConnectionManager(int localPort, JTextArea textArea, String IPAddress, int remotePort){
        init(localPort, textArea);
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.parent = new Peer(IPAddress, remotePort);
        this.senderManager = new SenderManager(textArea, outgoingEvents, false);
        new Thread(senderManager).start();
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

    private void initClientThreads(Socket socket) {
        TextEventSender sender = new TextEventSender(socket);
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender, this);
        Utility.startRunnable(sender);
        Utility.startRunnable(receiver);
        senderManager.addSender(sender);
    }

    public void initParentConnection(String IPAddress, int portNumber) {
        Socket rootSocket = Utility.connectToServer(IPAddress, portNumber);
        toRootSender = new TextEventSender(rootSocket, incomingEvents);
        TextEventReceiver receiver = new TextEventReceiver(rootSocket, outgoingEvents, toRootSender, this);
        Utility.startRunnable(toRootSender);
        Utility.startRunnable(receiver);
    }

    public void redirectTo(Peer peer){
        if(peer.equals(me)) return;
        initParentConnection(peer.getIPAddress(), peer.getPortNumber());
        parent = peer;
    }






    @Override
    public void disconnect() throws InterruptedException {
        if(parent != null){
            outgoingEvents.put(new RedirectEvent(parent));
            outgoingEvents.put(new ShutDownEvent(false));
            incomingEvents.put(new ShutDownEvent(false));
            Utility.deregisterOnPort(serverSocket);
            serverSocket = null;
            parent = null;
        } else {
            handleRootDisconnect();
        }


    }

    private void handleRootDisconnect() {
        redirectSender = senderManager.getRedirectSender();
        try {
            redirectSender.put(new RootAssignEvent(0, false));
        } catch (InterruptedException e) {}


    }

    public void setNewRoot(Peer peer) {
        System.out.println("SetNewRoot called");
        try{
            incomingEvents.put(new RedirectEvent(peer));
            System.out.println("RedirectEvent sent to children");
            int maxReceivedTimestamp = senderManager.getMaxReceivedTimestamp();
            ConcurrentHashMap<Integer,MyTextEvent> eventLog = senderManager.getEventLog();
            RootAssignEvent rae = new RootAssignEvent(maxReceivedTimestamp, true);
            rae.setEventLog(eventLog);
            redirectSender.put(rae);
            System.out.println("RootAssignEvent sent to redirect peer");
            incomingEvents.put(new ShutDownEvent(false));
            System.out.println("ShutDownEvent sent to children of root");
        } catch (InterruptedException e) {}
        Utility.deregisterOnPort(serverSocket);
        serverSocket = null;

    }

    public void beginInitAsRoot() {
        System.out.println("BeingInitAsRoot called");
        try{
            toRootSender.put(new RootAssignAckEvent(new Peer(Utility.getLocalHostAddress(), localPort)));
            System.out.println("RootAssignAckEvent sent");
        } catch (InterruptedException e){}

    }

    public void finishInitAsRoot(RootAssignEvent rae) {
        System.out.println("FinishedAsRoot called");
        parent = null;
        outgoingEvents = null;
        toRootSender = null;
        LinkedBlockingQueue<EventSender> senders = senderManager.getSenders();
        senderManager = new SenderManager(textArea, incomingEvents, true, senders, rae);
        Utility.startRunnable(senderManager);
        System.out.println("I have been assigned to become Root");
    }


}
