/**
 * Created by Jeppe Vinberg on 05-05-2016.
 */
public class TextSyncEvent extends MyTextEvent {

    static final long serialVersionUID = 2L;
    private String areaText;

    public TextSyncEvent(String areaText) {
        super("", -1, 0, -1);
        this.areaText = areaText;
    }

    public String getAreaText(){
        return areaText;
    }




}
