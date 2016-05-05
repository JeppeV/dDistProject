/**
 * Created by Jeppe Vinberg on 05-05-2016.
 */
public class TextSyncEvent extends MyTextEvent {

    private String areaText;

    public TextSyncEvent(String text) {
        super("", -1, 0, -1);
        this.areaText = text;
    }

    public String getAreaText(){
        return areaText;
    }




}
