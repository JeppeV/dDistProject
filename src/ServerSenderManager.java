
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 30-04-2016.
 */
public class ServerSenderManager implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> events;
    private LinkedBlockingQueue<TextEventSender> senders;
    private int timestamp;

    public ServerSenderManager(LinkedBlockingQueue<MyTextEvent> events){
        this.events = events;
        this.senders = new LinkedBlockingQueue<>();
        this.timestamp = 0;
    }

    @Override
    public void run() {
        MyTextEvent event;
        try{
            while(true){
                event = events.take();
                event.setTimestamp(timestamp++);
                for(TextEventSender sender : senders){
                    sender.put(event);
                }
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void addSender(TextEventSender sender) throws InterruptedException{
        senders.put(sender);
    }


}
