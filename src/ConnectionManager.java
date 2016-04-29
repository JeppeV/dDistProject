import java.awt.event.TextEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * For a peer acting as server, the resposibility of the ConnectionManager is to await
 * incoming connections from clients, and delegate a TextEventSender and TextEventCapturer to handle communication with
 * this socket. Instances of TextEventCapturer share a reference to a LinkedBlockingQueue, which allows the EventReplayer
 * to replay text events from several clients.
 */
public class ConnectionManager implements Runnable {

    private ServerSocket serverSocket;
    private DocumentEventCapturer documentEventCapturer;
    private LinkedBlockingQueue<MyTextEvent> events;
    private TextEventSenderManager senderManager;
    private LinkedBlockingQueue<TextEventSender> textEventSenders;

    public ConnectionManager(ServerSocket serverSocket, DocumentEventCapturer documentEventCapturer,
                                   LinkedBlockingQueue<MyTextEvent> events) {
        this.serverSocket = serverSocket;
        this.documentEventCapturer = documentEventCapturer;
        this.events = events;
        this.textEventSenders = new LinkedBlockingQueue<>();
        this.senderManager = new TextEventSenderManager(textEventSenders, events);

    }

    @Override
    public void run() {
        Socket socket;
        init();
        try{
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
        }catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    private void init(){
        Thread t = new Thread(senderManager);
        t.start();
    }

    private void initClientThreads(Socket socket) throws InterruptedException {
        TextEventSender sender = new TextEventSender(socket);
        TextEventReceiver receiver = new TextEventReceiver(socket, events, documentEventCapturer);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        textEventSenders.put(sender);
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
}
