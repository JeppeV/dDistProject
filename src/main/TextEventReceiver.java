package main;

import events.MyTextEvent;
import events.RedirectEvent;
import events.RootAssignAckEvent;
import events.RootAssignEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by frederik290 on 15/04/16.
 * <p>
 * The resposibility of the TextEventReceiver is to await incoming objects from the related socket.
 * These text event objects are then put onto a shared LinkedBlockingQueue.
 * It is then the EventReplayer's resposibility to take the text events off the queue and replay them in text area 2.
 */

public class TextEventReceiver implements Runnable {

    private Socket socket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private TextEventSender sender;
    private LamportClock lamportClock;
    private ConnectionManager connectionManager;

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, TextEventSender sender, ConnectionManager connectionManager) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.sender = sender;
        this.lamportClock = null;
        this.connectionManager = null;
        this.connectionManager = connectionManager;
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

                if (textEvent instanceof ShutDownEvent) {
                    ShutDownEvent e = (ShutDownEvent) textEvent;
                    shutdown = e.getShutdown();
                    if (!shutdown) {
                        e.setShutdown(true);
                        //initiate termination of corresponding sender thread
                        sender.put(e);
                    }
                    break;
                } else if(textEvent instanceof RedirectEvent){
                    if(connectionManager != null){
                        RedirectEvent r = (RedirectEvent) textEvent;
                        connectionManager.redirectTo(r.getPeer());
                    }
                } else if(textEvent instanceof RootAssignEvent) {
                    if(connectionManager != null) {
                        RootAssignEvent e = (RootAssignEvent) textEvent;
                        System.out.println("Received RootAssignEvent, with: " + e.assignIsFinished());
                        if (!e.assignIsFinished()) {
                            connectionManager.beginInitAsRoot();
                        } else if (e.assignIsFinished()) {
                            connectionManager.finishInitAsRoot(e);
                        }
                    }
                } else if(textEvent instanceof RootAssignAckEvent) {
                    System.out.println("RootAssignAckEvent received");
                    RootAssignAckEvent e = (RootAssignAckEvent) textEvent;
                    connectionManager.setNewRoot(e.getPeer());
                    shutdown = false;
                    break;
                }

                else {
                    if (lamportClock != null) {
                        lamportClock.processTimestamp(textEvent.getTimestamp());
                    }
                    incomingEvents.put(textEvent);
                }
            }
            if (shutdown) {
                objectInputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace(); //TODO
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); //TODO
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO
        }

    }
}
