import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by frederik290 on 15/04/16.
 * <p>
 * The resposibility of the TextEventSender is to await new text event from the DocumentEventCapturer.
 * When a new object is caught by the DocumentEventCapturer, the TextEventSender takes the object off the queue and sends it
 * to the corresponding socket.
 */
public class TextEventSender implements Runnable {
    private DocumentEventCapturer documentEventCapturer;
    private Socket socket;
    boolean shutdown;

    public TextEventSender(DocumentEventCapturer documentEventCapturer, Socket socket) {
        this.documentEventCapturer = documentEventCapturer;
        this.socket = socket;
    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            while (true) {
                textEvent = documentEventCapturer.take();
                objectOutputStream.writeObject(textEvent);
                if (textEvent instanceof ShutDownTextEvent) {
                    shutdown = ((ShutDownTextEvent) textEvent).getShutdown();
                    break;
                }
            }
            if (shutdown) {
                objectOutputStream.close();
            }

            System.out.println("Sender terminated");

        } catch (IOException e) {
            //TODO
        } catch (InterruptedException e) {
            //TODO
        }
    }
}
