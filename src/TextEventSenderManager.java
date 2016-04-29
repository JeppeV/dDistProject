
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 29-04-2016.
 */
public class TextEventSenderManager implements Runnable {

    private LinkedBlockingQueue<TextEventSender> textEventSenders;
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;

    public TextEventSenderManager(LinkedBlockingQueue<TextEventSender> textEventSenders, LinkedBlockingQueue<MyTextEvent> events) {
        this.textEventSenders = textEventSenders;
        this.outgoingEvents = events;
    }

    @Override
    public void run() {
        MyTextEvent event;
        while(true){
            try{
                event = outgoingEvents.take();
                for(TextEventSender s : textEventSenders){
                    s.put(event);
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }
    }

}
