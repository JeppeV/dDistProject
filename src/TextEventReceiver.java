
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 15/04/16.
 * <p>
 * The resposibility of the TextEventReceiver is to await incoming objects from the related socket.
 * These text event objects are then put onto a shared LinkedBlockingQueue.
 * It is then the EventReplayer's resposibility to take the text events off the queue and replay them in text area 2.
 */

public class TextEventReceiver implements Runnable {

    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private TextEventSender sender;
    private ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap;

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, TextEventSender sender, ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.sender = sender;
        this.senderMap = senderMap;
    }

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, TextEventSender sender) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.sender = sender;
        this.senderMap = null;
    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        boolean shutdown;
        ObjectInputStream objectInputStream;

        try {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            while (true) {
                textEvent = (MyTextEvent) objectInputStream.readObject();
                if (textEvent instanceof ShutDownTextEvent) {
                    ShutDownTextEvent e = (ShutDownTextEvent) textEvent;
                    shutdown = e.getShutdown();
                    if (!shutdown) {
                        e.setShutdown(true);
                        //initiate termination of corresponding sender thread
                        sender.put(e);
                    }
                    break;
                } else {
                    if (senderMap != null) senderMap.put(textEvent, sender);
                    incomingEvents.put(textEvent);
                }
            }
            if (shutdown) {
                objectInputStream.close();
            }
            System.out.println("Receiver terminated");

        } catch (IOException e) {
            e.printStackTrace(); //TODO
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //TODO
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO
        }

    }
}
