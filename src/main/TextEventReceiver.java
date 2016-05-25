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
    private ConnectionManager connectionManager;

    public TextEventReceiver(Socket socket, LinkedBlockingQueue<MyTextEvent> incomingEvents, TextEventSender sender, ConnectionManager connectionManager) {
        this.socket = socket;
        this.incomingEvents = incomingEvents;
        this.sender = sender;
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
                    shutdown = handleShutDownEvent(e);
                    break;

                } else if(textEvent instanceof RedirectEvent){
                    RedirectEvent e = (RedirectEvent) textEvent;
                    if(handleRedirectEvent(e)) {
                        shutdown = true;
                        break;
                    }

                } else if(textEvent instanceof RootAssignEvent) {
                    RootAssignEvent e = (RootAssignEvent) textEvent;
                    handleRootAssignEvent(e);

                } else if(textEvent instanceof RootAssignAckEvent) {
                    RootAssignAckEvent e = (RootAssignAckEvent) textEvent;
                    handleRootAssignAckEvent(e);
                    shutdown = false;
                    break;

                } else {
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

    private boolean handleShutDownEvent(ShutDownEvent event) throws InterruptedException {
        boolean shutdown = event.getShutdown();
        if (!shutdown) {
            event.setShutdown(true);
            //initiate termination of corresponding sender thread
            sender.put(event);
        }
        return shutdown;
    }

    private boolean handleRedirectEvent(RedirectEvent event) throws InterruptedException {
        if(connectionManager.getThisPeer().equals(event.getPeer())) return false;
        sender.put(new ShutDownEvent(false));
        connectionManager.redirectTo(event.getPeer());
        return true;

    }

    private void handleRootAssignEvent(RootAssignEvent event) {
        System.out.println("Received RootAssignEvent, with: " + event.assignIsFinished());
        if (!event.assignIsFinished()) {
            connectionManager.beginInitAsRoot();
        } else if (event.assignIsFinished()) {
            connectionManager.finishInitAsRoot(event);
        }
    }

    private void handleRootAssignAckEvent(RootAssignAckEvent event) {
        System.out.println("RootAssignAckEvent received");
        connectionManager.setNewRoot(event.getPeer());
    }

}
