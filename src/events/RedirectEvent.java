package events;


import main.Peer;

/**
 * Created by frederik290 on 23/05/16.
 */
public class RedirectEvent extends MyTextEvent{
    static final long serialVersionUID = 5L;

    private Peer peer;

    public RedirectEvent(Peer peer){
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
