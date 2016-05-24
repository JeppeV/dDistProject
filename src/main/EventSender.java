package main;

import events.MyTextEvent;

/**
 * Created by frederik290 on 24/05/16.
 */
public interface EventSender {

    void put(MyTextEvent event) throws InterruptedException;

    boolean isTerminated();
}
