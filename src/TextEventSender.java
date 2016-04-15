import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by frederik290 on 15/04/16.
 */
public class TextEventSender implements Runnable {
    private DocumentEventCapturer documentEventCapturer;
    private Socket socket;

    public TextEventSender(DocumentEventCapturer documentEventCapturer, Socket socket){
        this.documentEventCapturer = documentEventCapturer;
        this.socket = socket;
    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        ObjectOutputStream objectOutputStream;
        try{
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            while (true){
                textEvent = documentEventCapturer.take();
                objectOutputStream.writeObject(textEvent);
            } // break for null
        } catch (IOException e){
            //TODO
        } catch (InterruptedException e) {
            //TODO
        }
    }
}
