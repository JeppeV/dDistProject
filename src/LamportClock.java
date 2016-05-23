/**
 * Created by frederik290 on 19/05/16.
 */

public class LamportClock {

    private volatile int time;

    public LamportClock() {
        this.time = 0;
    }

    public int getTime() {
        return time;
    }

    public int generateTimestamp(){
        return ++time;
    }

    public void processTimestamp(int timestamp) {
        time = Math.max(time, timestamp) + 1;
    }
}
