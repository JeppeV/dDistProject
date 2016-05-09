/**
 * Created by Jeppe Vinberg on 05-05-2016.
 *
 * This class is a special implementation of MyTextEvent that is used
 * when a client goes out of sync. In this case, the whole text area text
 * is sent to the client for him to be properly synchronized.
 */
public class TextSyncEvent extends MyTextEvent {

    static final long serialVersionUID = 3L;
    private String areaText;

    public TextSyncEvent(int offset, String areaText) {
        super("", -1, 0, offset);
        this.areaText = areaText;
    }

    public String getAreaText() {
        return areaText;
    }

    public int getLength(){
        return areaText.length();
    }


}
