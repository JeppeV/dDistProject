import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * For a peer acting as server, the resposibility of the ServerConnectionManager is to await
 * incoming connections from clients, and delegate a TextEventSender and TextEventCapturer to handle communication with
 * this socket. Instances of TextEventCapturer share a reference to a LinkedBlockingQueue, which allows the EventReplayer
 * to replay text events from several clients.
 */
public class ServerConnectionManager implements Runnable, DisconnectHandler {

    private ServerSocket serverSocket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents, outgoingEvents;
    private ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap;
    private ServerEventReplayer serverEventReplayer;
    private ServerSenderManager serverSenderManager;
    private JTextArea serverTextArea;

    public ServerConnectionManager(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = new LinkedBlockingQueue<>();
        this.senderMap = new ConcurrentHashMap<>();
        this.serverTextArea = new JTextArea();
        this.serverEventReplayer = new ServerEventReplayer(incomingEvents, outgoingEvents, serverTextArea, senderMap);
        this.serverSenderManager = new ServerSenderManager(outgoingEvents);
        new Thread(serverEventReplayer).start();
        new Thread(serverSenderManager).start();


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
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender, senderMap);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        try {
            serverSenderManager.addSender(sender, serverTextArea);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        outgoingEvents.put(new ShutDownTextEvent(false));
    }
}
