/**
 * Created by Jeppe Vinberg on 09-05-2016.
 */
public class LamportClock {

    private int time;

    public LamportClock(){
        this.time = 0;
    }

    public int getTime(){
        return time;
    }

    public int generateTimestamp(){
        return ++time;
    }

    public void processTimestamp(int timestamp){
        time = Math.max(time, timestamp);
    }
}
