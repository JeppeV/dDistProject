package main;

import events.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 30-04-2016.
 * <p>
 * The resposibility of this class is to manage the sender theads for several clients.
 * If an event is put onto the outgoingEvents queue, this runnable will distribute this event to each
 * of its associated TextEventSenders.
 * When a new TextEventSender is added, all of the servers text area is sent to the client as the first event.
 */
public class SenderManager implements Runnable {

    private JTextArea textArea;
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private LinkedBlockingQueue<EventSender> senders;
    private ConcurrentHashMap<Integer, MyTextEvent> eventLog;
    private int maxReceivedTimestamp;
    private boolean isRoot;

    public SenderManager(JTextArea textArea, LinkedBlockingQueue<MyTextEvent> outgoingEvents, boolean isRoot) {
        this.textArea = textArea;
        this.outgoingEvents = outgoingEvents;
        this.senders = new LinkedBlockingQueue<>();
        this.eventLog = new ConcurrentHashMap<>();
        this.maxReceivedTimestamp = -1;
        this.isRoot = isRoot;
    }

    public SenderManager(JTextArea textArea, LinkedBlockingQueue<MyTextEvent> outgoingEvents, boolean isRoot, LinkedBlockingQueue<EventSender> senders, RootAssignEvent rootAssignEvent){
        this(textArea, outgoingEvents, isRoot);
        this.senders = senders;
        this.eventLog = rootAssignEvent.getEventLog();
        this.maxReceivedTimestamp = rootAssignEvent.getTimestamp();
        System.out.println("New SenderManager created");
    }

    public LinkedBlockingQueue<EventSender> getSenders() {
        return senders;
    }

    @Override
    public void run() {
        MyTextEvent event;
        try {
            while (true) {
                event = outgoingEvents.take();

                if(isRoot){
                    event = adjustOffset(event);
                }

                adjustMaxReceivedTimestamp(event);

                for (EventSender sender : senders) {
                    sender.put(event);
                }
                if(event instanceof ShutDownEvent) {
                    System.out.println("SenderManager process terminated");
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addSender(EventSender sender) {
        try{
            //send all of text area to new client
            sender.put(new InitTextEvent(maxReceivedTimestamp, textArea.getText()));
            senders.put(sender);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    public int getMaxReceivedTimestamp() {
        return maxReceivedTimestamp;
    }

    public EventSender getRedirectSender() {
        for(EventSender sender : senders) {
            if(sender instanceof TextEventSender && !sender.isTerminated()) {
                return sender;
            }
        }
        return null;
    }

    public ConcurrentHashMap<Integer,MyTextEvent> getEventLog() {
        return eventLog;
    }


    private void adjustMaxReceivedTimestamp(MyTextEvent event){
        maxReceivedTimestamp = Math.max(event.getTimestamp(), maxReceivedTimestamp);
    }

    private MyTextEvent adjustOffset(MyTextEvent event) {
        MyTextEvent e;
        while ((e = eventLog.get(event.getTimestamp())) != null || (event.getTimestamp() < maxReceivedTimestamp)) {
            if (e != null) {
                if (e.getOffset() <= event.getOffset()) {
                    if (e instanceof TextInsertEvent) {
                        event.setOffset(event.getOffset() + e.getLength());
                    } else if (e instanceof TextRemoveEvent) {
                        event.setOffset(event.getOffset() - e.getLength());
                    }
                }
            }
            event.setTimestamp(event.getTimestamp() + 1);
        }
        eventLog.put(event.getTimestamp(), event);
        return event;
    }


}
