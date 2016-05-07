/**
 * Created by Jeppe Vinberg on 05-05-2016.
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


}
