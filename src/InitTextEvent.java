/**
 * Created by Jeppe Vinberg on 23-05-2016.
 */
public class InitTextEvent extends MyTextEvent {

    static final long serialVersionUID = 7L;

    private String text;

    public InitTextEvent(int timestamp, String text){
        super(timestamp, 0);
        this.text = text;
    }

    public String getText(){
        return text;
    }

    @Override
    public int getLength() {
        return text.length();
    }
}
