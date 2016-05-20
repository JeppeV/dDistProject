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
 * this socket. It is also responsible for initializing all server related processes and objects.
 */
public class ServerConnectionManager implements Runnable, DisconnectHandler {


    private ServerSocket serverSocket; // The ServerSocket related to this server
    private LinkedBlockingQueue<MyTextEvent> incomingEvents; // A shared queue for exchanging incoming events between threads
    private ServerSenderManager serverSenderManager; // A thread for managing the Sender threads of several clients
    private JTextArea textArea;

    public ServerConnectionManager(ServerSocket serverSocket, JTextArea textArea) {
        this.serverSocket = serverSocket;
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.serverSenderManager = new ServerSenderManager(incomingEvents);
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
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        try {
            serverSenderManager.addSender(sender, textArea);
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
        incomingEvents.put(new ShutDownTextEvent(false));
    }
}
