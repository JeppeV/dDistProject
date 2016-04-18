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
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, DocumentEventCapturer documentEventCapturer){
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.documentEventCapturer = documentEventCapturer;

    }

    @Override
    public void run() {
        MyTextEvent textEvent;
        boolean shutdown;
        ObjectInputStream objectInputStream;
        try{
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            while(true){
                textEvent = (MyTextEvent) objectInputStream.readObject();
                if(textEvent instanceof ShutDownTextEvent){
                    ShutDownTextEvent e = (ShutDownTextEvent) textEvent;
                    shutdown = e.getShutdown();
                    if(!shutdown){
                        e.setShutdown(true);
                        //initiate termination of corresponding sender thread
                        documentEventCapturer.put(textEvent);
                    }
                    break;
                }else{
                    incomingEvents.put(textEvent);
                }

            }
            if(shutdown){
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
