package events;

import main.Peer;

/**
 * Created by Jeppe Vinberg on 24-05-2016.
 */
public class RootAssignAckEvent extends MyTextEvent {

    static final long serialVersionUID = 9L;

    private Peer peer;

    public RootAssignAckEvent(Peer peer){
        super(0, 0);
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    @Override
    public int getLength() {
        return 0;
    }
}
