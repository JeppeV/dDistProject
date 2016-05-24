/**
 * Created by frederik290 on 24/05/16.
 */
public interface EventSender {

    public void put(MyTextEvent event) throws InterruptedException;
}
