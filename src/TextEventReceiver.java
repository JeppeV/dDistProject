import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 15/04/16.
 * <p>
 * The resposibility of the TextEventReceiver is to await incoming objects from the related socket.
 * These text event objects are then put onto a shared LinkedBlockingQueue.
 * It is then the EventReplayer's resposibility to take the text events off the queue and replay them in text area 2.
 */

public class TextEventReceiver implements Runnable {

    private TextEventSender sender;
    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private LinkedList<MyTextEvent> outstandingEvents;
    private int expectedTimestamp;

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, TextEventSender sender) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.sender = sender;
        this.outstandingEvents = new LinkedList<>();
        this.expectedTimestamp = 0;

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
                    System.out.println("Received shutdowntext event with: " + shutdown);
                    if (!shutdown) {
                        e.setShutdown(true);
                        //initiate termination of corresponding sender thread
                        sender.put(e);
                    }
                    break;
                } else if (textEvent instanceof InitialTextEvent) {
                    expectedTimestamp = textEvent.getTimestamp();

                }

                else {
                    processEvent(textEvent);
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

    private void processEvent(MyTextEvent event) throws InterruptedException{
        int timestamp = event.getTimestamp();

        if(timestamp == expectedTimestamp){
            incomingEvents.put(event);
            expectedTimestamp++;

            if(!outstandingEvents.isEmpty()){
                MyTextEvent e;
                while((e = getNextOutstandingEvent()) != null){
                    incomingEvents.put(e);
                    expectedTimestamp++;
                }
            }
        } else{
            outstandingEvents.add(event);
        }

    }

    private MyTextEvent getNextOutstandingEvent(){
        MyTextEvent event = null;
        int index = -1;
        for(int i = 0; i < outstandingEvents.size(); i++){
            event = outstandingEvents.get(i);
            if(event.getTimestamp() == expectedTimestamp) {
                index = i;
                break;
            }
        }
        if(index > -1){
            outstandingEvents.remove(index);
            return event;
        }
        return null;
    }
}
