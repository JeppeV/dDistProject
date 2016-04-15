import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 15/04/16.
 */
public class TextEventReceiver implements Runnable {
    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> eventHistory = new LinkedBlockingQueue<>();

    public TextEventReceiver(Socket socket){
        this.socket = socket;
    }

    public MyTextEvent take() throws InterruptedException {
        return eventHistory.take();
    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        try{
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            while(true){
                textEvent = (MyTextEvent) objectInputStream.readObject();
                eventHistory.put(textEvent);
            }
        } catch (IOException e) {
            e.printStackTrace(); //TODO
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
