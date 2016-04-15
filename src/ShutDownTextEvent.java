/**
 * Created by Jeppe Vinberg on 15-04-2016.
 */
public class ShutDownTextEvent extends MyTextEvent {

    private boolean shutdown;

    public ShutDownTextEvent(boolean shutdown) {
        super(0);
        this.shutdown = shutdown;
    }

    public void setShutdown(boolean s){
        this.shutdown = s;
    }

    public boolean getShutdown(){
        return shutdown;
    }
}
