import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 15/04/16.
 */

public class TextEventReceiver implements Runnable {

    private DocumentEventCapturer documentEventCapturer;
    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> eventHistory = new LinkedBlockingQueue<>();

    public TextEventReceiver(DocumentEventCapturer documentEventCapturer, Socket socket){
        this.documentEventCapturer = documentEventCapturer;
        this.socket = socket;
    }

    public MyTextEvent take() throws InterruptedException {
        return eventHistory.take();
    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        boolean shutdown;
        try{
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            while(true){
                textEvent = (MyTextEvent) objectInputStream.readObject();
                if(textEvent instanceof ShutDownTextEvent){
                    ShutDownTextEvent e = (ShutDownTextEvent) textEvent;
                    shutdown = e.getShutdown();
                    if(!shutdown){
                        e.setShutdown(true);
                        //initiate termination of sender thread
                        documentEventCapturer.put(textEvent);
                    }
                    //initiate termination of event replayer thread
                    eventHistory.put(textEvent);
                    break;
                }else{
                    eventHistory.put(textEvent);
                }

            }
            if(shutdown){
                socket.close();
            }
            System.out.println("Receiver terminated");

        } catch (IOException e) {
            e.printStackTrace(); //TODO
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
